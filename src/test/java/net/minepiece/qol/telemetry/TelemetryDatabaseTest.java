package net.minepiece.qol.telemetry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryDatabaseTest {
    @TempDir
    Path tempDir;

    @Test
    void storesSessionsEventsAndDurableOutbox() throws Exception {
        Path databasePath = this.tempDir.resolve("history.sqlite");
        try (TelemetryDatabase database = new TelemetryDatabase(databasePath)) {
            database.startSession("s1", 100L, "play.minepiece.net", "1.21.11", "1.1.0");
            database.insertEvents(List.of(new TelemetryEvent(
                "s1", 1L, 110L, TelemetryEvent.Category.ECONOMY, "money_change", "{\"signed_amount\":200}"
            )));
            database.finishSession("s1", 200L, true, 0L);
            database.enqueueDiscord(200L, "summary", "done", "");

            assertEquals(1, database.events("s1").size());
            assertTrue(database.session("s1").cleanEnd());
            assertEquals(1, database.pendingOutboxCount());
            assertNotNull(database.nextOutbox(200L));
            assertEquals("", database.nextOutbox(200L).payloadJson());
        }
        assertTrue(Files.isRegularFile(databasePath));
    }

    @Test
    void migratesExistingPlainTextOutboxForStructuredPayloads() throws Exception {
        Path databasePath = this.tempDir.resolve("old-history.sqlite");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE discord_outbox (id INTEGER PRIMARY KEY AUTOINCREMENT, created_ms INTEGER NOT NULL, "
                + "next_attempt_ms INTEGER NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, kind TEXT NOT NULL, content TEXT NOT NULL, "
                + "attachment_path TEXT NOT NULL DEFAULT '', last_error TEXT NOT NULL DEFAULT '')");
            statement.execute("INSERT INTO discord_outbox(created_ms,next_attempt_ms,kind,content) VALUES(1,1,'old','plain message')");
        }

        try (TelemetryDatabase database = new TelemetryDatabase(databasePath)) {
            TelemetryDatabase.OutboxEntry old = database.nextOutbox(1L);
            assertNotNull(old);
            assertEquals("plain message", old.content());
            assertEquals("", old.payloadJson());
            database.enqueueDiscord(2L, "new", "", "{\"embeds\":[]}", "report.xlsx");
            database.completeOutbox(old.id());
            assertEquals("{\"embeds\":[]}", database.nextOutbox(2L).payloadJson());
        }
    }

    @Test
    void exportsReadableLocalAndDiscordSafeWorkbookWithHiddenRawData() throws Exception {
        try (TelemetryDatabase database = new TelemetryDatabase(this.tempDir.resolve("history.sqlite"))) {
            database.startSession("s1", 100L, "play.minepiece.net", "1.21.11", "1.1.0");
            database.insertEvents(List.of(
                new TelemetryEvent("s1", 1L, 110L, TelemetryEvent.Category.CHAT_COMMAND, "chat", "{\"message\":\"hello\"}"),
                new TelemetryEvent("s1", 2L, 120L, TelemetryEvent.Category.INVENTORY, "slot_change",
                    "{\"item_id\":\"minecraft:netherite_sword\",\"name\":\"Big Mom's Napoleon\",\"count\":1,"
                        + "\"previous_count\":0,\"inventory\":\"player\",\"slot\":0,"
                        + "\"components\":{\"serialized\":\"technical-code\"}}")
            ));
            database.finishSession("s1", 200L, true, 0L);
            Path local = TelemetryWorkbookExporter.export(database, this.tempDir, "s1", false);
            Path safe = TelemetryWorkbookExporter.export(database, this.tempDir, "s1", true);

            assertTrue(local.getFileName().toString().matches("MP-\\d{4}-\\d{2}-\\d{2}-1-full\\.xlsx"));
            assertTrue(safe.getFileName().toString().matches("MP-\\d{4}-\\d{2}-\\d{2}-1\\.xlsx"));

            try (ZipFile localZip = new ZipFile(local.toFile()); ZipFile safeZip = new ZipFile(safe.toFile())) {
                String localWorkbook = new String(localZip.getInputStream(localZip.getEntry("xl/workbook.xml")).readAllBytes());
                String safeWorkbook = new String(safeZip.getInputStream(safeZip.getEntry("xl/workbook.xml")).readAllBytes());
                assertTrue(localWorkbook.contains("Overview"));
                assertTrue(localWorkbook.contains("Timeline"));
                assertTrue(localWorkbook.contains("Inventory"));
                assertTrue(localWorkbook.contains("Economy"));
                assertTrue(localWorkbook.contains("Combat"));
                assertTrue(localWorkbook.contains("Interactions"));
                assertTrue(localWorkbook.contains("Raw Data"));
                assertTrue(localWorkbook.contains("state=\"hidden\""));
                assertTrue(zipContains(localZip, "hello"));
                assertFalse(zipContains(safeZip, "hello"));
                assertTrue(zipContains(safeZip, "technical-code"));
                assertTrue(zipContains(safeZip, "Napoleon"));
                assertTrue(zipContains(safeZip, "autoFilter"));
                assertTrue(zipContains(safeZip, "pane"));
            }
        }
    }

    private static boolean zipContains(ZipFile zip, String text) throws Exception {
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (!entry.isDirectory() && new String(zip.getInputStream(entry).readAllBytes()).contains(text)) {
                return true;
            }
        }
        return false;
    }
}
