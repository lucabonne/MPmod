package net.minepiece.qol.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minepiece.qol.i18n.UiLocalization;
import net.minepiece.qol.state.PersistentState;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> SUPPORTED_TRANSLATION_LANG_CODES =
        List.of("en", "fr", "es", "de", "it", "pt", "pl", "id", "tr");

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
        if (config.bossTrackingEnabled == null) {
            config.bossTrackingEnabled = true;
        }
        if (config.minibossHudEnabled == null) {
            config.minibossHudEnabled = true;
        }
        if (config.scrollsEnabled == null) {
            config.scrollsEnabled = true;
        }
        if (config.jobsHudColor == null || config.jobsHudColor.isBlank()) {
            config.jobsHudColor = "default";
        }
        if (config.moneyHudColor == null || config.moneyHudColor.isBlank()) {
            config.moneyHudColor = "default";
        }
        if (config.statsHudColor == null || config.statsHudColor.isBlank()) {
            config.statsHudColor = "default";
        }
        if (config.bossHudColor == null || config.bossHudColor.isBlank()) {
            config.bossHudColor = "default";
        }
        if (config.minibossHudColor == null || config.minibossHudColor.isBlank()) {
            config.minibossHudColor = "default";
        }
        if (config.eventsHudColor == null || config.eventsHudColor.isBlank()) {
            config.eventsHudColor = "default";
        }
        if (config.hakiHudColor == null || config.hakiHudColor.isBlank()) {
            config.hakiHudColor = "default";
        }
        if (config.scrollsHudColor == null || config.scrollsHudColor.isBlank()) {
            config.scrollsHudColor = "default";
        }
        if (config.inventoryXpHudColor == null || config.inventoryXpHudColor.isBlank()) {
            config.inventoryXpHudColor = "default";
        }
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
        if (config.minibossHudScale <= 0.0F) {
            config.minibossHudScale = 1.0F;
        }
        if (config.eventsHudScale <= 0.0F) {
            config.eventsHudScale = 1.0F;
        }
        if (config.hakiHudScale <= 0.0F) {
            config.hakiHudScale = 0.9F;
        }
        if (config.scrollsHudScale <= 0.0F) {
            config.scrollsHudScale = 1.0F;
        }
        if (config.inventoryXpHudScale <= 0.0F) {
            config.inventoryXpHudScale = 1.0F;
        }
        if (config.chatTranslationRules == null) {
            config.chatTranslationRules = new ArrayList<>();
        }
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            config.debugEnabled = false;
        }
        if (config.menuKeybindKey == 0) {
            config.menuKeybindKey = org.lwjgl.glfw.GLFW.GLFW_KEY_PERIOD;
        }
        config.uiLanguage = UiLocalization.normalizeLanguageCode(config.uiLanguage);
        LinkedHashMap<String, ModConfig.ChatTranslationRule> dedupedRules = new LinkedHashMap<>();
        for (ModConfig.ChatTranslationRule rule : config.chatTranslationRules) {
            if (rule == null) {
                continue;
            }
            String source = normalizeTranslationLanguageCode(rule.sourceLanguage, "fr");
            String target = normalizeTranslationLanguageCode(rule.targetLanguage, "en");
            if (source.equals(target)) {
                continue;
            }
            String key = source + "->" + target;
            if (dedupedRules.containsKey(key)) {
                continue;
            }
            ModConfig.ChatTranslationRule normalizedRule = new ModConfig.ChatTranslationRule();
            normalizedRule.enabled = rule.enabled;
            normalizedRule.sourceLanguage = source;
            normalizedRule.targetLanguage = target;
            dedupedRules.put(key, normalizedRule);
        }
        config.chatTranslationRules = new ArrayList<>(dedupedRules.values());
    }

    private static String normalizeTranslationLanguageCode(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            normalized = normalized.substring(0, dash);
        }
        int underscore = normalized.indexOf('_');
        if (underscore > 0) {
            normalized = normalized.substring(0, underscore);
        }
        if (!SUPPORTED_TRANSLATION_LANG_CODES.contains(normalized)) {
            return fallback;
        }
        return normalized;
    }

    public static final class ModConfig {
        public boolean modEnabled = true;
        public boolean allFeaturesVisible = true;
        public boolean debugEnabled = false;
        public Boolean bossTrackingEnabled = true;
        public Boolean minibossHudEnabled = true;
        public boolean minibossTrackingEnabled = true;
        public boolean moneyTrackingEnabled = true;
        public boolean jobsTrackingEnabled = true;
        public boolean eventsEnabled = true;
        public boolean profileStatsEnabled = true;
        public boolean auctionHighlightEnabled = true;
        public boolean hakiEnabled = true;
        public Boolean scrollsEnabled = true;
        public boolean inventoryXpHudEnabled = true;
        public boolean chatTranslationEnabled = false;
        public boolean chatTranslationAggressiveEnabled = false;
        public boolean chatTranslationPublicEnabled = true;
        public boolean chatTranslationPrivateEnabled = true;
        public boolean chatTranslationSystemEnabled = false;
        public String uiLanguage = UiLocalization.DEFAULT_LANGUAGE;
        public boolean rarityIconsEnabled = true;
        public boolean petStatIconsEnabled = true;
        public List<ChatTranslationRule> chatTranslationRules = new ArrayList<>();
        public int menuKeybindKey = org.lwjgl.glfw.GLFW.GLFW_KEY_PERIOD;
        public int bossHudX = 8;
        public int bossHudY = 8;
        public int statsHudX = 8;
        public int statsHudY = 44;
        public int jobsHudX = 8;
        public int jobsHudY = 80;
        public float jobsHudScale = 1.0F;
        public String jobsHudColor = "default";
        public boolean jobsOverviewVisible = true;
        public int moneyHudX = 8;
        public int moneyHudY = 116;
        public float moneyHudScale = 1.0F;
        public String moneyHudColor = "default";
        public int minibossHudX = 220;
        public int minibossHudY = 8;
        public float minibossHudScale = 1.0F;
        public String minibossHudColor = "default";
        public int eventsHudX = 8;
        public int eventsHudY = 152;
        public float eventsHudScale = 1.0F;
        public String eventsHudColor = "default";
        public float statsHudScale = 1.0F;
        public String statsHudColor = "default";
        public float bossHudScale = 1.0F;
        public String bossHudColor = "default";
        public int hakiHudX = -1;
        public int hakiHudY = -1;
        public float hakiHudScale = 0.9F;
        public String hakiHudColor = "default";
        public int scrollsHudX = 8;
        public int scrollsHudY = 188;
        public float scrollsHudScale = 1.0F;
        public String scrollsHudColor = "default";
        public int inventoryXpHudX = 8;
        public int inventoryXpHudY = 224;
        public float inventoryXpHudScale = 1.0F;
        public String inventoryXpHudColor = "default";
        public int cooldownHudOffsetY = 54;

        public static final class ChatTranslationRule {
            public boolean enabled = true;
            public String sourceLanguage = "fr";
            public String targetLanguage = "en";
        }
    }

}
