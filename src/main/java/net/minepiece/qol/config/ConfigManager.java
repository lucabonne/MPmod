package net.minepiece.qol.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minepiece.qol.state.PersistentState;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path baseDir;
    private final Path configPath;
    private final Path statePath;

    public ConfigManager() {
        this.baseDir = FabricLoader.getInstance().getConfigDir().resolve("minepiece-qol");
        this.configPath = this.baseDir.resolve("config.json");
        this.statePath = this.baseDir.resolve("state.json");
    }

    public void ensureDirectories() {
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException ignored) {
        }
    }

    public Path getBaseDir() {
        return this.baseDir;
    }

    public ModConfig loadConfig() {
        ModConfig config = load(this.configPath, ModConfig.class, new ModConfig());
        normalizeConfig(config);
        return config;
    }

    public void saveConfig(ModConfig config) {
        normalizeConfig(config);
        save(this.configPath, config);
    }

    public PersistentState loadState() {
        return load(this.statePath, PersistentState.class, new PersistentState());
    }

    public void saveState(PersistentState state) {
        save(this.statePath, state);
    }

    private <T> T load(Path path, Class<T> type, T fallback) {
        ensureDirectories();
        if (!Files.exists(path)) {
            save(path, fallback);
            return fallback;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            T value = GSON.fromJson(reader, type);
            return value == null ? fallback : value;
        } catch (IOException | RuntimeException ignored) {
            return fallback;
        }
    }

    private void save(Path path, Object data) {
        ensureDirectories();
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }

    private static void normalizeConfig(ModConfig config) {
        if (config.jobsHudScale <= 0.0F) {
            config.jobsHudScale = 1.0F;
        }
        if (config.moneyHudScale <= 0.0F) {
            config.moneyHudScale = 1.0F;
        }
        if (config.statsHudScale <= 0.0F) {
            config.statsHudScale = 1.0F;
        }
        if (config.bossHudScale <= 0.0F) {
            config.bossHudScale = 1.0F;
        }
        if (config.eventsHudScale <= 0.0F) {
            config.eventsHudScale = 1.0F;
        }
        if (config.hakiHudScale <= 0.0F) {
            config.hakiHudScale = 0.9F;
        }
    }

    public static final class ModConfig {
        public boolean allFeaturesVisible = true;
        public boolean debugEnabled = false;
        public boolean bossWaypointEnabled = false;
        public boolean autoProfileRefresh = false;
        public int bossHudX = 8;
        public int bossHudY = 8;
        public int statsHudX = 8;
        public int statsHudY = 44;
        public int jobsHudX = 8;
        public int jobsHudY = 80;
        public float jobsHudScale = 1.0F;
        public boolean jobsOverviewVisible = true;
        public int moneyHudX = 8;
        public int moneyHudY = 116;
        public float moneyHudScale = 1.0F;
        public int eventsHudX = 8;
        public int eventsHudY = 152;
        public float eventsHudScale = 1.0F;
        public float statsHudScale = 1.0F;
        public float bossHudScale = 1.0F;
        public int hakiHudX = -1;
        public int hakiHudY = -1;
        public float hakiHudScale = 0.9F;
        public int cooldownHudOffsetY = 54;
    }

}
