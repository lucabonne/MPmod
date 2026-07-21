package net.minepiece.qol.telemetry;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

final class TelemetryDatabase implements AutoCloseable {
    private final Connection connection;

    TelemetryDatabase(Path path) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        try (Statement statement = this.connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("CREATE TABLE IF NOT EXISTS sessions ("
                + "id TEXT PRIMARY KEY, started_ms INTEGER NOT NULL, ended_ms INTEGER, server TEXT NOT NULL, "
                + "client_version TEXT NOT NULL, mod_version TEXT NOT NULL, clean_end INTEGER NOT NULL DEFAULT 0, dropped_events INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS events ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, session_id TEXT NOT NULL REFERENCES sessions(id), sequence INTEGER NOT NULL, "
                + "timestamp_ms INTEGER NOT NULL, category TEXT NOT NULL, type TEXT NOT NULL, data_json TEXT NOT NULL, "
                + "UNIQUE(session_id, sequence))");
            statement.execute("CREATE INDEX IF NOT EXISTS events_session_time ON events(session_id, timestamp_ms)");
            statement.execute("CREATE INDEX IF NOT EXISTS events_category ON events(category, type)");
            statement.execute("CREATE TABLE IF NOT EXISTS discord_outbox ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, created_ms INTEGER NOT NULL, next_attempt_ms INTEGER NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, "
                + "kind TEXT NOT NULL, content TEXT NOT NULL, payload_json TEXT NOT NULL DEFAULT '', "
                + "attachment_path TEXT NOT NULL DEFAULT '', last_error TEXT NOT NULL DEFAULT '')");
            ensureColumn(statement, "discord_outbox", "payload_json", "TEXT NOT NULL DEFAULT ''");
            statement.execute("PRAGMA user_version=2");
        }
    }

    synchronized void recoverInterruptedSessions(long now) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(
            "UPDATE sessions SET ended_ms=?, clean_end=0 WHERE ended_ms IS NULL")) {
            statement.setLong(1, now);
            statement.executeUpdate();
        }
    }

    synchronized void startSession(String id, long startedMs, String server, String clientVersion, String modVersion) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(
            "INSERT INTO sessions(id,started_ms,server,client_version,mod_version) VALUES(?,?,?,?,?)")) {
            statement.setString(1, id);
            statement.setLong(2, startedMs);
            statement.setString(3, server);
            statement.setString(4, clientVersion);
            statement.setString(5, modVersion);
            statement.executeUpdate();
        }
    }

    synchronized void finishSession(String id, long endedMs, boolean clean, long droppedEvents) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(
            "UPDATE sessions SET ended_ms=?, clean_end=?, dropped_events=? WHERE id=?")) {
            statement.setLong(1, endedMs);
            statement.setInt(2, clean ? 1 : 0);
            statement.setLong(3, droppedEvents);
            statement.setString(4, id);
            statement.executeUpdate();
        }
    }

    synchronized void insertEvents(List<TelemetryEvent> events) throws SQLException {
        if (events.isEmpty()) {
            return;
        }
        boolean autoCommit = this.connection.getAutoCommit();
        this.connection.setAutoCommit(false);
        try (PreparedStatement statement = this.connection.prepareStatement(
            "INSERT OR IGNORE INTO events(session_id,sequence,timestamp_ms,category,type,data_json) VALUES(?,?,?,?,?,?)")) {
            for (TelemetryEvent event : events) {
                statement.setString(1, event.sessionId());
                statement.setLong(2, event.sequence());
                statement.setLong(3, event.timestampMs());
                statement.setString(4, event.category().name());
                statement.setString(5, event.type());
                statement.setString(6, event.dataJson());
                statement.addBatch();
            }
            statement.executeBatch();
            this.connection.commit();
        } catch (SQLException exception) {
            this.connection.rollback();
            throw exception;
        } finally {
            this.connection.setAutoCommit(autoCommit);
        }
    }

    synchronized List<StoredEvent> events(String sessionId) throws SQLException {
        String sql = sessionId == null
            ? "SELECT session_id,sequence,timestamp_ms,category,type,data_json FROM events ORDER BY timestamp_ms,session_id,sequence"
            : "SELECT session_id,sequence,timestamp_ms,category,type,data_json FROM events WHERE session_id=? ORDER BY sequence";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            if (sessionId != null) {
                statement.setString(1, sessionId);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<StoredEvent> events = new ArrayList<>();
                while (result.next()) {
                    events.add(new StoredEvent(
                        result.getString(1), result.getLong(2), result.getLong(3), result.getString(4), result.getString(5), result.getString(6)
                    ));
                }
                return events;
            }
        }
    }

    synchronized SessionRecord session(String id) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(
            "SELECT id,started_ms,ended_ms,server,client_version,mod_version,clean_end,dropped_events FROM sessions WHERE id=?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readSession(result) : null;
            }
        }
    }

    synchronized List<SessionRecord> sessions() throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(
            "SELECT id,started_ms,ended_ms,server,client_version,mod_version,clean_end,dropped_events FROM sessions ORDER BY started_ms")) {
            try (ResultSet result = statement.executeQuery()) {
                List<SessionRecord> sessions = new ArrayList<>();
                while (result.next()) {
                    sessions.add(readSession(result));
                }
                return sessions;
            }
        }
    }

    synchronized void enqueueDiscord(long now, String kind, String content, String attachmentPath) throws SQLException {
        enqueueDiscord(now, kind, content, "", attachmentPath);
    }

    synchronized void enqueueDiscord(long now, String kind, String content, String payloadJson, String attachmentPath) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(
            "INSERT INTO discord_outbox(created_ms,next_attempt_ms,kind,content,payload_json,attachment_path) VALUES(?,?,?,?,?,?)")) {
            statement.setLong(1, now);
            statement.setLong(2, now);
            statement.setString(3, kind);
            statement.setString(4, content);
            statement.setString(5, payloadJson == null ? "" : payloadJson);
            statement.setString(6, attachmentPath == null ? "" : attachmentPath);
            statement.executeUpdate();
        }
    }

    synchronized OutboxEntry nextOutbox(long now) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(
            "SELECT id,attempts,kind,content,payload_json,attachment_path FROM discord_outbox "
                + "WHERE attempts<10 AND next_attempt_ms<=? ORDER BY id LIMIT 1")) {
            statement.setLong(1, now);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new OutboxEntry(
                    result.getLong(1), result.getInt(2), result.getString(3), result.getString(4), result.getString(5), result.getString(6)
                ) : null;
            }
        }
    }

    synchronized void completeOutbox(long id) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement("DELETE FROM discord_outbox WHERE id=?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    synchronized void failOutbox(long id, int attempts, long nextAttempt, String error) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement(
            "UPDATE discord_outbox SET attempts=?,next_attempt_ms=?,last_error=? WHERE id=?")) {
            statement.setInt(1, attempts);
            statement.setLong(2, nextAttempt);
            statement.setString(3, error == null ? "" : error.substring(0, Math.min(300, error.length())));
            statement.setLong(4, id);
            statement.executeUpdate();
        }
    }

    synchronized int pendingOutboxCount() throws SQLException {
        try (Statement statement = this.connection.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM discord_outbox")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        this.connection.close();
    }

    private static SessionRecord readSession(ResultSet result) throws SQLException {
        long ended = result.getLong(3);
        return new SessionRecord(
            result.getString(1), result.getLong(2), result.wasNull() ? null : ended, result.getString(4),
            result.getString(5), result.getString(6), result.getInt(7) != 0, result.getLong(8)
        );
    }

    private static void ensureColumn(Statement statement, String table, String column, String definition) throws SQLException {
        try (ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) return;
            }
        }
        statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    record StoredEvent(String sessionId, long sequence, long timestampMs, String category, String type, String dataJson) {
    }

    record SessionRecord(String id, long startedMs, Long endedMs, String server, String clientVersion, String modVersion,
                         boolean cleanEnd, long droppedEvents) {
    }

    record OutboxEntry(long id, int attempts, String kind, String content, String payloadJson, String attachmentPath) {
    }
}
