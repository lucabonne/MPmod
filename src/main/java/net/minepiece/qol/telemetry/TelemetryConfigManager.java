package net.minepiece.qol.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public final class TelemetryConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<PosixFilePermission> OWNER_ONLY = Set.of(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE
    );

    private final Path baseDir;
    private final Path configPath;

    public TelemetryConfigManager(Path modConfigDir) {
        this.baseDir = modConfigDir.resolve("telemetry");
        this.configPath = modConfigDir.resolve("telemetry-config.json");
    }

    public TelemetryConfig load() {
        ensureDirectories();
        if (!Files.exists(this.configPath)) {
            TelemetryConfig config = new TelemetryConfig();
            save(config);
            return config;
        }
        try (Reader reader = Files.newBufferedReader(this.configPath)) {
            TelemetryConfig config = GSON.fromJson(reader, TelemetryConfig.class);
            return config == null ? new TelemetryConfig() : config;
        } catch (IOException | RuntimeException ignored) {
            return new TelemetryConfig();
        }
    }

    public void save(TelemetryConfig config) {
        ensureDirectories();
        try (Writer writer = Files.newBufferedWriter(this.configPath)) {
            GSON.toJson(config, writer);
        } catch (IOException ignored) {
            return;
        }
        applyOwnerOnlyPermissions(this.configPath);
    }

    public Path databasePath() {
        return this.baseDir.resolve("history.sqlite");
    }

    public Path exportsDir() {
        return this.baseDir.resolve("exports");
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(this.baseDir);
            Files.createDirectories(exportsDir());
        } catch (IOException ignored) {
        }
    }

    private static void applyOwnerOnlyPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, OWNER_ONLY);
        } catch (IOException | UnsupportedOperationException ignored) {
        }
    }
}
