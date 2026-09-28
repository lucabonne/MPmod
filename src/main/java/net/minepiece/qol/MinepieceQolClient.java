package net.minepiece.qol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.io.InputStream;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minepiece.qol.commands.ModCommands;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.i18n.UiLocalization;
import net.minepiece.qol.mixin.PlayerListHudAccessor;
import net.minepiece.qol.parse.ChatParsers;
import net.minepiece.qol.parse.MoneyMessageClassifier;
import net.minepiece.qol.parse.TooltipParsers;
import net.minepiece.qol.state.AuctionHighlighter;
import net.minepiece.qol.state.BossGuiAutoScanner;
import net.minepiece.qol.state.BossTracker;
import net.minepiece.qol.state.ChatTranslationManager;
import net.minepiece.qol.state.CooldownTracker;
import net.minepiece.qol.state.DebugLogManager;
import net.minepiece.qol.state.EventCountdownTracker;
import net.minepiece.qol.state.InventoryXpTracker;
import net.minepiece.qol.state.JobsTracker;
import net.minepiece.qol.state.MoneyTracker;
import net.minepiece.qol.state.PersistentState;
import net.minepiece.qol.state.ProfileStatsTracker;
import net.minepiece.qol.state.ProgressHudController;
import net.minepiece.qol.state.CookingTracker;
import net.minepiece.qol.state.RarityDetector;
import net.minepiece.qol.state.ScrollTracker;
import net.minepiece.qol.state.StatsRefreshController;
import net.minepiece.qol.ui.AuctionTooltipFormatter;
import net.minepiece.qol.ui.HudOverlay;
import net.minepiece.qol.ui.HudLayoutScreen;
import net.minepiece.qol.ui.MinepieceMenuScreen;
import net.minepiece.qol.ui.MinibossWaypointWorldRenderer;
import net.minepiece.qol.util.LocalizedText;
import net.minepiece.qol.util.SafeExecutor;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MinepieceQolClient implements ClientModInitializer {
    private static final List<String> HUD_COLOR_OPTIONS = List.of(
        "default",
        "blue",
        "red",
        "purple",
        "yellow",
        "pink",
        "green",
        "orange",
        "lime",
        "aqua",
        "navy",
        "coral",
        "teal",
        "mustard",
        "blue violet",
        "black",
        "white",
        "grey",
        "brown",
        "dark green",
        "blue gray",
        "indigo",
        "pea green",
        "amber",
        "peach",
        "maroon"
    );
    private static MinepieceQolClient instance;

    private ConfigManager configManager;
    private ConfigManager.ModConfig config;
    private PersistentState persistentState;

    private DebugLogManager debugLogManager;
    private AuctionHighlighter auctionHighlighter;
    private BossTracker bossTracker;
    private BossGuiAutoScanner bossGuiAutoScanner;
    private CooldownTracker cooldownTracker;
    private ProfileStatsTracker profileStatsTracker;
    private StatsRefreshController statsRefreshController;
    private JobsTracker jobsTracker;
    private MoneyTracker moneyTracker;
    private EventCountdownTracker eventCountdownTracker;
    private ScrollTracker scrollTracker;
    private InventoryXpTracker inventoryXpTracker;
    private final CookingTracker cookingTracker = new CookingTracker();
    private final ProgressHudController progressHudController = new ProgressHudController();
    private HudOverlay hudOverlay;
    private MinibossWaypointWorldRenderer minibossWaypointWorldRenderer;
    private ChatTranslationManager chatTranslationManager;
    private KeyBinding openMenuKeyBinding;
    private final net.minepiece.qol.state.ChatChannelTracker chatChannelTracker = new net.minepiece.qol.state.ChatChannelTracker();

    public net.minepiece.qol.state.ChatChannelTracker getChatChannelTracker() { return this.chatChannelTracker; }

    private KeyBinding loadoutKeyBinding;
    private final net.minepiece.qol.ui.CustomPictures customPictures = new net.minepiece.qol.ui.CustomPictures();

    public net.minepiece.qol.ui.CustomPictures getCustomPictures() { return this.customPictures; }
    public boolean isHudPanelVisible(int id) { return !this.config.appearance.hiddenPanels.contains(id); }
    public net.minepiece.qol.config.UiSettings.Picture getHudPicture(int id) {
        int index = id - 12;
        return index >= 0 && index < this.config.appearance.pictures.size() ? this.config.appearance.pictures.get(index) : null;
    }


    private final Map<Integer, String> lastAuctionDebugLines = new HashMap<>();
    private String lastTabFooter = "";
    private boolean hudEditMode;
    private int hudEditSelectedPanel = 1;
    private String talkTarget = "";
    private boolean menuOpenHintShown;

    public static MinepieceQolClient get() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        System.out.println("MinepieceQOL CLIENT INITIALIZED");

        this.configManager = new ConfigManager();
        this.configManager.ensureDirectories();
        this.config = this.configManager.loadConfig();
        this.persistentState = this.configManager.loadState();
        this.progressHudController.attachPersistence(this.persistentState, this::savePersistentState);
        boolean debugEnabled = isDebugToolsAvailable() && this.config.debugEnabled;
        if (this.config.debugEnabled != debugEnabled) {
            this.config.debugEnabled = debugEnabled;
            this.configManager.saveConfig(this.config);
        }

        this.debugLogManager = new DebugLogManager(this.configManager.getBaseDir(), debugEnabled);
        this.auctionHighlighter = new AuctionHighlighter();
        this.chatTranslationManager = new ChatTranslationManager(this.debugLogManager);
        this.bossTracker = new BossTracker(this.persistentState, this::savePersistentState, this.debugLogManager);
        this.bossGuiAutoScanner = new BossGuiAutoScanner(this.bossTracker, this.debugLogManager);
        this.bossTracker.setMinibossRegistrationEnabled(isBossTrackingEnabled() && this.config.minibossTrackingEnabled);
        this.cooldownTracker = new CooldownTracker();
        this.profileStatsTracker = new ProfileStatsTracker();
        this.profileStatsTracker.attachPersistence(this.persistentState, this::savePersistentState);
        this.statsRefreshController = new StatsRefreshController(
            this.profileStatsTracker,
            this::sendProfileCommand,
            this.debugLogManager,
            this::tr
        );
        this.jobsTracker = new JobsTracker(this.persistentState, this::savePersistentState, this.debugLogManager);
        this.moneyTracker = new MoneyTracker(this.persistentState, this::savePersistentState, this.configManager.getBaseDir());
        this.eventCountdownTracker = new EventCountdownTracker(this.persistentState, this::savePersistentState);
        this.scrollTracker = new ScrollTracker(this.debugLogManager);
        this.inventoryXpTracker = new InventoryXpTracker(this.debugLogManager);
        this.hudOverlay = new HudOverlay(this);
        this.minibossWaypointWorldRenderer = new MinibossWaypointWorldRenderer(this);

        this.openMenuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.minepiece-qol.open_menu",
            InputUtil.Type.KEYSYM,
            this.config.menuKeybindKey,
            KeyBinding.Category.create(Identifier.of("minepiece-qol", "main"))
        ));

        net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback.EVENT.register(data ->
            data instanceof net.minepiece.qol.ui.ItemDetailTooltipData detail
                ? new net.minepiece.qol.ui.IconGridTooltipComponent(detail.rows(), this.config.appearance.compact, this.config.appearance.shadows)
                : null);
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) ->
            SafeExecutor.run(this.debugLogManager, "tooltip", () -> this.handleTooltip(stack, lines)));
        this.loadoutKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.minepiece-qol.loadout", InputUtil.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSLASH,
            KeyBinding.Category.create(Identifier.of("minepiece-qol", "loadouts"))));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ModCommands.register(dispatcher, this));
        ClientSendMessageEvents.ALLOW_CHAT.register(this::handleOutgoingChatMessage);
        ClientSendMessageEvents.COMMAND.register(command -> {
            this.moneyTracker.onCommandSent(command);
            this.onCommandSent(command);
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            String normalized = TextUtil.normalize(message);
            this.moneyTracker.onPlayerChatMessage(normalized);
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String normalized = TextUtil.normalize(message);
            if (!overlay) this.chatChannelTracker.onServerMessage(normalized);
            if (MoneyMessageClassifier.isEligibleServerMessage(normalized, overlay)) {
                this.moneyTracker.onGameMessage(normalized);
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String server = client.getCurrentServerEntry() == null ? "local" : client.getCurrentServerEntry().address;
            this.progressHudController.connect(server + "|" + client.getSession().getUuidOrNull(), System.currentTimeMillis());
            this.inventoryXpTracker.clear();
            this.cookingTracker.clear();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            this.chatChannelTracker.reset();
            this.progressHudController.disconnect(System.currentTimeMillis());
            this.inventoryXpTracker.clear();
            this.cookingTracker.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            this.progressHudController.persist(System.currentTimeMillis());
            this.customPictures.clear();
            savePersistentState(this.persistentState);
            if (this.chatTranslationManager != null) {
                this.chatTranslationManager.shutdown();
            }
        });
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.SUBTITLES,
            Identifier.of("minepiece-qol", "main_hud"),
            (drawContext, tickCounter) -> this.hudOverlay.render(drawContext)
        );
        this.minibossWaypointWorldRenderer.register();
    }

    public ConfigManager.ModConfig getConfig() {
        return this.config;
    }

    public BossTracker getBossTracker() {
        return this.bossTracker;
    }

    public CooldownTracker getCooldownTracker() {
        return this.cooldownTracker;
    }

    public ProfileStatsTracker getProfileStatsTracker() {
        return this.profileStatsTracker;
    }

    public StatsRefreshController getStatsRefreshController() {
        return this.statsRefreshController;
    }

    public JobsTracker getJobsTracker() {
        return this.jobsTracker;
    }

    public MoneyTracker getMoneyTracker() {
        return this.moneyTracker;
    }

    public EventCountdownTracker getEventCountdownTracker() {
        return this.eventCountdownTracker;
    }

    public ScrollTracker getScrollTracker() {
        return this.scrollTracker;
    }

    public InventoryXpTracker getInventoryXpTracker() {
        return this.inventoryXpTracker;
    }

    public List<String> getXpHudLines() {
        List<String> lines = new ArrayList<>();
        if (this.config.profileXpHudEnabled) {
            lines.addAll(this.progressHudController.profile().getHudLines(this::tr));
        }
        if (this.config.inventoryXpHudEnabled) {
            lines.addAll(this.inventoryXpTracker.getHudLines(this::tr));
        }
        return lines;
    }

    public CookingTracker getCookingTracker() {
        return this.cookingTracker;
    }

    public ProgressHudController getProgressHudController() {
        return this.progressHudController;
    }

    public boolean isHudEditMode() {
        return this.hudEditMode;
    }

    public int getHudEditSelectedPanel() {
        return this.hudEditSelectedPanel;
    }

    public void setHudEditSelectedPanel(int panelId) {
        this.hudEditSelectedPanel = Math.max(1, Math.min(getHudPanelCount(), panelId));
    }

    public int getHudPanelCount() {
        return 11 + this.config.appearance.pictures.size();
    }

    public String getHudPanelName(int panelId) {
        return switch (panelId) {
            case 0 -> tr("hud.panel.chat");
            case 1 -> tr("hud.panel.jobs");
            case 2 -> tr("hud.panel.money");
            case 3 -> tr("hud.panel.stats");
            case 4 -> tr("hud.panel.bosses");
            case 5 -> tr("hud.panel.minibosses");
            case 6 -> tr("hud.panel.event");
            case 7 -> tr("hud.panel.haki");
            case 8 -> tr("hud.panel.scrolls");
            case 9 -> tr("hud.panel.xp");
            case 10 -> tr("hud.panel.grinding");
            case 11 -> tr("hud.panel.cooking");
            default -> getHudPicture(panelId) == null ? tr("common.unknown") : getHudPicture(panelId).name;
        };
    }

    public int getHudPanelX(int panelId) {
        return switch (panelId) {
            case 1 -> this.config.jobsHudX;
            case 2 -> this.config.moneyHudX;
            case 3 -> this.config.statsHudX;
            case 4 -> this.config.bossHudX;
            case 5 -> this.config.minibossHudX;
            case 6 -> this.config.eventsHudX;
            case 7 -> this.config.hakiHudX;
            case 8 -> this.config.scrollsHudX;
            case 9 -> this.config.inventoryXpHudX;
            case 10 -> this.config.grindingHudX;
            case 11 -> this.config.cookingHudX;
            default -> getHudPicture(panelId) == null ? 0 : getHudPicture(panelId).x;
        };
    }

    public int getHudPanelY(int panelId) {
        return switch (panelId) {
            case 1 -> this.config.jobsHudY;
            case 2 -> this.config.moneyHudY;
            case 3 -> this.config.statsHudY;
            case 4 -> this.config.bossHudY;
            case 5 -> this.config.minibossHudY;
            case 6 -> this.config.eventsHudY;
            case 7 -> this.config.hakiHudY;
            case 8 -> this.config.scrollsHudY;
            case 9 -> this.config.inventoryXpHudY;
            case 10 -> this.config.grindingHudY;
            case 11 -> this.config.cookingHudY;
            default -> getHudPicture(panelId) == null ? 0 : getHudPicture(panelId).y;
        };
    }

    public float getHudPanelScale(int panelId) {
        return switch (panelId) {
            case 1 -> this.config.jobsHudScale;
            case 2 -> this.config.moneyHudScale;
            case 3 -> this.config.statsHudScale;
            case 4 -> this.config.bossHudScale;
            case 5 -> this.config.minibossHudScale;
            case 6 -> this.config.eventsHudScale;
            case 7 -> this.config.hakiHudScale;
            case 8 -> this.config.scrollsHudScale;
            case 9 -> this.config.inventoryXpHudScale;
            case 10 -> this.config.grindingHudScale;
            case 11 -> this.config.cookingHudScale;
            default -> getHudPicture(panelId) == null ? 1.0F : getHudPicture(panelId).scale;
        };
    }

    public void setHudPanelLayout(int panelId, int x, int y, float scale) {
        float clampedScale = Float.isFinite(scale) ? Math.max(0.6F, Math.min(1.8F, scale)) : 1.0F;
        var picture = getHudPicture(panelId);
        if (picture != null) { picture.x = x; picture.y = y; picture.scale = clampedScale; return; }
        switch (panelId) {
            case 1 -> {
                this.config.jobsHudX = x;
                this.config.jobsHudY = y;
                this.config.jobsHudScale = clampedScale;
            }
            case 2 -> {
                this.config.moneyHudX = x;
                this.config.moneyHudY = y;
                this.config.moneyHudScale = clampedScale;
            }
            case 3 -> {
                this.config.statsHudX = x;
                this.config.statsHudY = y;
                this.config.statsHudScale = clampedScale;
            }
            case 4 -> {
                this.config.bossHudX = x;
                this.config.bossHudY = y;
                this.config.bossHudScale = clampedScale;
            }
            case 5 -> {
                this.config.minibossHudX = x;
                this.config.minibossHudY = y;
                this.config.minibossHudScale = clampedScale;
            }
            case 6 -> {
                this.config.eventsHudX = x;
                this.config.eventsHudY = y;
                this.config.eventsHudScale = clampedScale;
            }
            case 7 -> {
                this.config.hakiHudX = x;
                this.config.hakiHudY = y;
                this.config.hakiHudScale = clampedScale;
            }
            case 8 -> {
                this.config.scrollsHudX = x;
                this.config.scrollsHudY = y;
                this.config.scrollsHudScale = clampedScale;
            }
            case 9 -> {
                this.config.inventoryXpHudX = x;
                this.config.inventoryXpHudY = y;
                this.config.inventoryXpHudScale = clampedScale;
            }
            case 11 -> {
                this.config.cookingHudX = x;
                this.config.cookingHudY = y;
                this.config.cookingHudScale = clampedScale;
            }
            case 10 -> {
                this.config.grindingHudX = x;
                this.config.grindingHudY = y;
                this.config.grindingHudScale = clampedScale;
            }
            default -> {
            }
        }
    }

    public void resetHudLayoutToDefaults() {
        ConfigManager.ModConfig defaults = new ConfigManager.ModConfig();
        this.config.bossHudX = defaults.bossHudX;
        this.config.bossHudY = defaults.bossHudY;
        this.config.bossHudScale = defaults.bossHudScale;

        this.config.statsHudX = defaults.statsHudX;
        this.config.statsHudY = defaults.statsHudY;
        this.config.statsHudScale = defaults.statsHudScale;

        this.config.jobsHudX = defaults.jobsHudX;
        this.config.jobsHudY = defaults.jobsHudY;
        this.config.jobsHudScale = defaults.jobsHudScale;

        this.config.moneyHudX = defaults.moneyHudX;
        this.config.moneyHudY = defaults.moneyHudY;
        this.config.moneyHudScale = defaults.moneyHudScale;

        this.config.minibossHudX = defaults.minibossHudX;
        this.config.minibossHudY = defaults.minibossHudY;
        this.config.minibossHudScale = defaults.minibossHudScale;

        this.config.eventsHudX = defaults.eventsHudX;
        this.config.eventsHudY = defaults.eventsHudY;
        this.config.eventsHudScale = defaults.eventsHudScale;

        this.config.hakiHudX = defaults.hakiHudX;
        this.config.hakiHudY = defaults.hakiHudY;
        this.config.hakiHudScale = defaults.hakiHudScale;

        this.config.scrollsHudX = defaults.scrollsHudX;
        this.config.scrollsHudY = defaults.scrollsHudY;
        this.config.scrollsHudScale = defaults.scrollsHudScale;

        this.config.inventoryXpHudX = defaults.inventoryXpHudX;
        this.config.inventoryXpHudY = defaults.inventoryXpHudY;
        this.config.inventoryXpHudScale = defaults.inventoryXpHudScale;


        this.config.cookingHudX = defaults.cookingHudX;
        this.config.cookingHudY = defaults.cookingHudY;
        this.config.cookingHudScale = defaults.cookingHudScale;
        this.config.grindingHudX = defaults.grindingHudX;
        this.config.grindingHudY = defaults.grindingHudY;
        this.config.grindingHudScale = defaults.grindingHudScale;

        this.configManager.saveConfig(this.config);
    }

    public String getHudPanelColor(int panelId) {
        return switch (panelId) {
            case 0 -> this.config.appearance.chatPanelColor;
            case 1 -> this.config.jobsHudColor;
            case 2 -> this.config.moneyHudColor;
            case 3 -> this.config.statsHudColor;
            case 4 -> this.config.bossHudColor;
            case 5 -> this.config.minibossHudColor;
            case 6 -> this.config.eventsHudColor;
            case 7 -> this.config.hakiHudColor;
            case 8 -> this.config.scrollsHudColor;
            case 9 -> this.config.inventoryXpHudColor;
            case 10 -> this.config.grindingHudColor;
            case 11 -> this.config.cookingHudColor;
            default -> "default";
        };
    }

    public boolean isHudPanelColorable(int panelId) {
        return panelId >= 0 && panelId <= 11;
    }

    public void cycleHudPanelColor(int panelId, int direction) {
        if (!isHudPanelColorable(panelId) || direction == 0) {
            return;
        }

        String current = normalizeHudColorName(getHudPanelColor(panelId));
        int currentIndex = HUD_COLOR_OPTIONS.indexOf(current);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        int step = direction > 0 ? 1 : -1;
        int nextIndex = Math.floorMod(currentIndex + step, HUD_COLOR_OPTIONS.size());
        setHudPanelColor(panelId, HUD_COLOR_OPTIONS.get(nextIndex));
    }

    public void resetHudPanelColor(int panelId) {
        setHudPanelColor(panelId, "default");
    }

    public void setHudPanelColor(int panelId, String colorName) {
        String normalized = normalizeHudColorName(colorName);
        switch (panelId) {
            case 0 -> this.config.appearance.chatPanelColor = normalized;
            case 1 -> this.config.jobsHudColor = normalized;
            case 2 -> this.config.moneyHudColor = normalized;
            case 3 -> this.config.statsHudColor = normalized;
            case 4 -> this.config.bossHudColor = normalized;
            case 5 -> this.config.minibossHudColor = normalized;
            case 6 -> this.config.eventsHudColor = normalized;
            case 7 -> this.config.hakiHudColor = normalized;
            case 8 -> this.config.scrollsHudColor = normalized;
            case 9 -> this.config.inventoryXpHudColor = normalized;
            case 10 -> this.config.grindingHudColor = normalized;
            case 11 -> this.config.cookingHudColor = normalized;
            default -> {
            }
        }
    }

    public static List<String> getHudColorOptions() {
        return HUD_COLOR_OPTIONS;
    }

    public static String normalizeHudColorName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "default";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("#?[0-9a-f]{6}")) return "#" + normalized.replace("#", "").toUpperCase(Locale.ROOT);
        return HUD_COLOR_OPTIONS.contains(normalized) ? normalized : "default";
    }

    public void saveConfig() {
        this.configManager.saveConfig(this.config);
    }

    public void onHudLayoutEditorClosed() {
        this.hudEditMode = false;
        this.configManager.saveConfig(this.config);
    }

    public int resolveHakiHudX(MinecraftClient client) {
        if (this.config.hakiHudX >= 0) {
            return this.config.hakiHudX;
        }
        int centerX = client.getWindow().getScaledWidth() / 2;
        int hotbarStartX = centerX - 91;
        return hotbarStartX - 17;
    }

    public int resolveHakiHudY(MinecraftClient client) {
        if (this.config.hakiHudY >= 0) {
            return this.config.hakiHudY;
        }
        return client.getWindow().getScaledHeight() - this.config.cooldownHudOffsetY - 9;
    }

    public DebugLogManager getDebugLogManager() {
        return this.debugLogManager;
    }

    public boolean isDebugToolsAvailable() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public void setDebugEnabled(boolean enabled) {
        boolean nextEnabled = isDebugToolsAvailable() && enabled;
        this.config.debugEnabled = nextEnabled;
        if (this.debugLogManager != null) {
            this.debugLogManager.setEnabled(nextEnabled);
        }
        this.configManager.saveConfig(this.config);
    }

    public String getUiLanguage() {
        return UiLocalization.normalizeLanguageCode(this.config.uiLanguage);
    }

    public List<UiLocalization.LanguageOption> getSupportedUiLanguages() {
        return UiLocalization.supportedLanguages();
    }

    public void setUiLanguage(String languageCode) {
        this.config.uiLanguage = UiLocalization.normalizeLanguageCode(languageCode);
        this.configManager.saveConfig(this.config);
    }

    public String tr(String key) {
        return UiLocalization.text(getUiLanguage(), key);
    }

    public List<ChatTranslationManager.LanguageOption> getSupportedChatLanguages() {
        return ChatTranslationManager.getSupportedLanguages();
    }

    public void setChatTranslationEnabled(boolean enabled) {
        this.config.chatTranslationEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setChatTranslationAggressiveEnabled(boolean enabled) {
        this.config.chatTranslationAggressiveEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setChatTranslationPublicEnabled(boolean enabled) {
        this.config.chatTranslationPublicEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setChatTranslationPrivateEnabled(boolean enabled) {
        this.config.chatTranslationPrivateEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setChatTranslationSystemEnabled(boolean enabled) {
        this.config.chatTranslationSystemEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void addChatTranslationRule(String sourceLanguage, String targetLanguage) {
        String source = ChatTranslationManager.normalizeLanguageCode(sourceLanguage);
        String target = ChatTranslationManager.normalizeLanguageCode(targetLanguage);
        if (!ChatTranslationManager.isSupportedLanguageCode(source)
            || !ChatTranslationManager.isSupportedLanguageCode(target)
            || source.equals(target)) {
            return;
        }
        for (ConfigManager.ModConfig.ChatTranslationRule rule : this.config.chatTranslationRules) {
            if (rule == null) {
                continue;
            }
            String existingSource = ChatTranslationManager.normalizeLanguageCode(rule.sourceLanguage);
            String existingTarget = ChatTranslationManager.normalizeLanguageCode(rule.targetLanguage);
            if (source.equals(existingSource) && target.equals(existingTarget)) {
                rule.enabled = true;
                this.configManager.saveConfig(this.config);
                return;
            }
        }

        ConfigManager.ModConfig.ChatTranslationRule newRule = new ConfigManager.ModConfig.ChatTranslationRule();
        newRule.enabled = true;
        newRule.sourceLanguage = source;
        newRule.targetLanguage = target;
        this.config.chatTranslationRules.add(newRule);
        this.configManager.saveConfig(this.config);
    }

    public void removeChatTranslationRule(int index) {
        if (index < 0 || index >= this.config.chatTranslationRules.size()) {
            return;
        }
        this.config.chatTranslationRules.remove(index);
        this.configManager.saveConfig(this.config);
    }

    public void setChatTranslationRuleEnabled(int index, boolean enabled) {
        if (index < 0 || index >= this.config.chatTranslationRules.size()) {
            return;
        }
        ConfigManager.ModConfig.ChatTranslationRule rule = this.config.chatTranslationRules.get(index);
        if (rule == null) {
            return;
        }
        rule.enabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void handleChatMessage(Text message) {
        String normalized = TextUtil.normalize(message);
        if (normalized.isBlank()) {
            return;
        }
        if (this.chatTranslationManager != null && this.chatTranslationManager.isSyntheticTranslationLine(normalized)) {
            return;
        }

        this.debugLogManager.logChat(normalized);
        SafeExecutor.run(this.debugLogManager, "chat", () -> {
            if (this.config.modEnabled && this.config.allFeaturesVisible && this.config.profileStatsEnabled) {
                this.statsRefreshController.onChatMessage(normalized);
            }
            this.jobsTracker.captureChatMessage(normalized);
            this.cooldownTracker.onChatMessage(normalized);
            ChatParsers.parseBossKill(normalized).ifPresent(name -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    int x = (int) Math.floor(client.player.getX());
                    int y = (int) Math.floor(client.player.getY());
                    int z = (int) Math.floor(client.player.getZ());
                    this.bossTracker.onBossKill(name, x, y, z);
                    return;
                }
                this.bossTracker.onBossKill(name);
            });
            if (this.config.modEnabled && this.config.allFeaturesVisible && this.config.chatTranslationEnabled
                && this.chatTranslationManager != null) {
                this.chatTranslationManager.onChatMessage(
                    normalized,
                    List.copyOf(this.config.chatTranslationRules),
                    this.config.chatTranslationPublicEnabled,
                    this.config.chatTranslationPrivateEnabled,
                    this.config.chatTranslationSystemEnabled,
                    this.config.chatTranslationAggressiveEnabled
                );
            }
        });
    }

    public void handleActionbarMessage(Text message) {
        String normalized = TextUtil.normalize(message);
        if (normalized.isBlank()) {
            return;
        }

        this.debugLogManager.logActionbar(normalized);
        SafeExecutor.run(this.debugLogManager, "actionbar", () -> {
            if (this.config.modEnabled && (this.config.profileXpHudEnabled || this.config.grindingHudEnabled)) {
                this.progressHudController.onActionbar(normalized, System.currentTimeMillis());
            }
            this.moneyTracker.onActionbarMessage(normalized);
            this.jobsTracker.captureActionbar(normalized);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                int x = (int) Math.floor(client.player.getX());
                int y = (int) Math.floor(client.player.getY());
                int z = (int) Math.floor(client.player.getZ());
                this.bossTracker.captureMinibossActionbar(normalized, x, y, z);
            }
        });
    }

    public void handleTitle(Text message) {
        String normalized = TextUtil.normalize(message);
        if (!normalized.isBlank()) {
            this.debugLogManager.logTitle(normalized);
            SafeExecutor.run(this.debugLogManager, "title", () -> {
                this.jobsTracker.captureVisibleText(normalized);
            });
        }
    }

    public void renderAuctionTint(DrawContext drawContext, int slotX, int slotY, int screenX, int screenY, Slot slot) {
        renderAuctionTint(drawContext, slotX, slotY, screenX, screenY, slot, null);
    }

    public void renderAuctionTint(DrawContext drawContext, int slotX, int slotY, int screenX, int screenY, Slot slot, List<Text> tooltipLines) {
        if (!this.config.modEnabled || !this.config.allFeaturesVisible || !this.config.auctionHighlightEnabled) {
            this.lastAuctionDebugLines.remove(slot.id);
            return;
        }

        Optional<AuctionHighlighter.ActiveHighlight> highlight = getAuctionHighlightForSlot(slot, tooltipLines);
        if (highlight.isEmpty()) {
            this.lastAuctionDebugLines.remove(slot.id);
            return;
        }

        AuctionHighlighter.ActiveHighlight activeHighlight = highlight.get();
        drawContext.fill(slotX, slotY, slotX + 16, slotY + 16, activeHighlight.argbColor());
        logAuctionHighlight(activeHighlight, slot, screenX, screenY);
    }

    public void clearAuctionTint() {
        this.lastAuctionDebugLines.clear();
        this.auctionHighlightRenderCache.clear();
    }

    public boolean isAuctionTintRenderEnabled() {
        return this.config != null
            && this.config.modEnabled
            && this.config.allFeaturesVisible
            && this.config.auctionHighlightEnabled;
    }

    public boolean isSlotIconRenderEnabled() {
        return this.config != null
            && this.config.modEnabled
            && this.config.allFeaturesVisible
            && (this.config.rarityIconsEnabled || this.config.petStatIconsEnabled);
    }

    public boolean isBossTooltipCaptureEnabled() {
        return this.config != null
            && this.config.modEnabled
            && this.config.allFeaturesVisible
            && isBossTrackingEnabled();
    }

    private static final int RARITY_ICON_SIZE = 6;
    private static final Identifier PET_STAT_TEX_STRENGTH = Identifier.of("minepiece-qol", "textures/gui/pet_stats/strength.png");
    private static final Identifier PET_STAT_TEX_POWER = Identifier.of("minepiece-qol", "textures/gui/pet_stats/power.png");
    private static final Identifier PET_STAT_TEX_CRIT_CHANCE = Identifier.of("minepiece-qol", "textures/gui/pet_stats/critical_chance.png");
    private static final Identifier PET_STAT_TEX_CRIT_DAMAGE = Identifier.of("minepiece-qol", "textures/gui/pet_stats/critical_damage.png");
    private static final Identifier PET_STAT_TEX_DEFENSE = Identifier.of("minepiece-qol", "textures/gui/pet_stats/defense.png");
    private static final Identifier PET_STAT_TEX_SPEED = Identifier.of("minepiece-qol", "textures/gui/pet_stats/speed.png");
    private static final Identifier PET_STAT_TEX_REGEN = Identifier.of("minepiece-qol", "textures/gui/pet_stats/regeneration.png");
    private static final Identifier PET_STAT_TEX_HEALTH = Identifier.of("minepiece-qol", "textures/gui/pet_stats/health.png");
    private static final Identifier PET_STAT_TEX_ENERGY = Identifier.of("minepiece-qol", "textures/gui/pet_stats/energy.png");
    private static final Identifier PET_STAT_TEX_ENERGY_REGEN = Identifier.of("minepiece-qol", "textures/gui/pet_stats/energy_regeneration.png");
    private static final Identifier PET_STAT_TEX_DEXTERITY = Identifier.of("minepiece-qol", "textures/gui/pet_stats/dexterity.png");
    private static final int PET_STAT_ICON_SIZE = 4;
    private static final int PET_STAT_MAX_ICONS = 4;
    private static final int PET_STAT_DEFAULT_TEXTURE_SIZE = 16;
    private static final int SLOT_OVERLAY_CACHE_MAX = 512;
    private static final Map<Identifier, Integer> PET_STAT_TEXTURE_SIZES = new HashMap<>();
    private final Map<String, Optional<AuctionHighlighter.ActiveHighlight>> auctionHighlightRenderCache = new HashMap<>();
    private final Map<String, Optional<RarityDetector.Rarity>> rarityIconRenderCache = new HashMap<>();
    private final Map<String, List<Identifier>> petStatIconRenderCache = new HashMap<>();

    public void renderRarityIcon(DrawContext drawContext, int slotX, int slotY, Slot slot) {
        renderRarityIcon(drawContext, slotX, slotY, slot, null);
    }

    public void renderRarityIcon(DrawContext drawContext, int slotX, int slotY, Slot slot, List<Text> tooltipLines) {
        if (!this.config.modEnabled || !this.config.allFeaturesVisible || !this.config.rarityIconsEnabled) {
            return;
        }
        if (slot == null || !slot.hasStack()) {
            return;
        }

        ItemStack stack = slot.getStack();
        String itemKey = buildAuctionItemKey(stack);
        Optional<RarityDetector.Rarity> cachedRarity = this.rarityIconRenderCache.get(itemKey);
        Optional<RarityDetector.Rarity> rarity;
        if (cachedRarity != null) {
            rarity = cachedRarity;
        } else {
            rarity = RarityDetector.detect(stack, tooltipLines);
            if (rarity.isEmpty()) {
                rarity = RarityDetector.detect(stack);
            }
        }

        if (cachedRarity == null && rarity.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                try {
                    List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
                    rarity = RarityDetector.detect(stack, tooltip);
                } catch (Throwable ignored) {
                    // Keep rendering fail-safe if tooltip construction fails on a malformed stack.
                }
            }
        }
        if (cachedRarity == null) {
            putBounded(this.rarityIconRenderCache, itemKey, rarity);
        }
        if (rarity.isEmpty()) {
            return;
        }

        net.minepiece.qol.ui.RarityBadges.draw(drawContext, rarity.get(), slotX + 16 - RARITY_ICON_SIZE, slotY, RARITY_ICON_SIZE);
    }

    public void setRarityIconsEnabled(boolean enabled) {
        this.config.rarityIconsEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setPetStatIconsEnabled(boolean enabled) {
        this.config.petStatIconsEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void renderPetStatIcons(DrawContext drawContext, int slotX, int slotY, Slot slot, List<Text> tooltipLines) {
        if (!this.config.modEnabled || !this.config.allFeaturesVisible || !this.config.petStatIconsEnabled) {
            return;
        }
        if (slot == null || !slot.hasStack()) {
            return;
        }

        ItemStack stack = slot.getStack();
        String itemKey = buildAuctionItemKey(stack);
        List<Identifier> cachedTextures = this.petStatIconRenderCache.get(itemKey);
        if (cachedTextures != null) {
            drawPetStatTextures(drawContext, slotX, slotY, cachedTextures);
            return;
        }

        List<Text> tooltip = tooltipLines;
        if (tooltip == null || tooltip.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) {
                return;
            }
            try {
                tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
            } catch (Throwable ignored) {
                return;
            }
        }
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }

        List<String> normalizedLines = TextUtil.normalizeLines(tooltip);
        Set<Identifier> textures = new LinkedHashSet<>();
        List<TooltipParsers.PetStatLine> petStats = TooltipParsers.parsePetRolls(
            normalizedLines,
            null,
            RarityDetector.detect(stack, tooltip).map(Enum::name).orElse("")
        );
        for (TooltipParsers.PetStatLine statLine : petStats) {
            Identifier texture = textureForPetStat(statLine.statName());
            if (texture == null) {
                continue;
            }
            textures.add(texture);
            if (textures.size() >= PET_STAT_MAX_ICONS) {
                break;
            }
        }

        if (textures.size() < PET_STAT_MAX_ICONS) {
            textures.addAll(findPetStatTexturesFromTooltip(normalizedLines, PET_STAT_MAX_ICONS - textures.size()));
        }

        if (textures.isEmpty()) {
            putBounded(this.petStatIconRenderCache, itemKey, List.of());
            return;
        }

        List<Identifier> textureList = List.copyOf(textures);
        putBounded(this.petStatIconRenderCache, itemKey, textureList);
        drawPetStatTextures(drawContext, slotX, slotY, textureList);
    }

    private static void drawPetStatTextures(DrawContext drawContext, int slotX, int slotY, List<Identifier> textures) {
        if (textures == null || textures.isEmpty()) {
            return;
        }
        int index = 0;
        int baseX = slotX + 1;
        int baseY = slotY;
        for (Identifier texture : textures) {
            int iconX = baseX;
            int iconY = baseY + index * PET_STAT_ICON_SIZE;
            int textureSize = resolvePetStatTextureSize(texture);
            float scale = PET_STAT_ICON_SIZE / (float) Math.max(1, textureSize);
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate((float) iconX, (float) iconY);
            drawContext.getMatrices().scale(scale, scale);
            drawContext.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                0,
                0,
                0.0F,
                0.0F,
                textureSize,
                textureSize,
                textureSize,
                textureSize
            );
            drawContext.getMatrices().popMatrix();
            index++;
            if (index >= PET_STAT_MAX_ICONS) {
                break;
            }
        }
    }

    private static int resolvePetStatTextureSize(Identifier texture) {
        Integer cached = PET_STAT_TEXTURE_SIZES.get(texture);
        if (cached != null) {
            return cached;
        }

        int resolved = PET_STAT_DEFAULT_TEXTURE_SIZE;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            try {
                Optional<net.minecraft.resource.Resource> resource = client.getResourceManager().getResource(texture);
                if (resource.isPresent()) {
                    try (InputStream input = resource.get().getInputStream()) {
                        byte[] header = input.readNBytes(24);
                        if (header.length >= 24 && header[0] == (byte) 0x89 && header[1] == 0x50
                            && header[2] == 0x4E && header[3] == 0x47) {
                            int width = ((header[16] & 0xFF) << 24)
                                | ((header[17] & 0xFF) << 16)
                                | ((header[18] & 0xFF) << 8)
                                | (header[19] & 0xFF);
                            int height = ((header[20] & 0xFF) << 24)
                                | ((header[21] & 0xFF) << 16)
                                | ((header[22] & 0xFF) << 8)
                                | (header[23] & 0xFF);
                            int min = Math.min(width, height);
                            if (min > 0) {
                                resolved = min;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        PET_STAT_TEXTURE_SIZES.put(texture, resolved);
        return resolved;
    }

    private static Set<Identifier> findPetStatTexturesFromTooltip(List<String> lines, int maxToAdd) {
        Set<Identifier> textures = new LinkedHashSet<>();
        if (maxToAdd <= 0) {
            return textures;
        }

        boolean inPetEffects = false;
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (LocalizedText.startsWithAny(lower,
                "pet effects", "familiar effects", "stats", "statistics", "estadisticas", "estadísticas",
                "effets du familier", "efectos de mascota", "efectos de familiar", "efectos del familiar",
                "haustier effekte", "vertrauten effekte", "effetti pet", "effetti famiglio", "efeitos do pet",
                "efeitos do familiar", "efekty peta", "efekty towarzysza", "efek pet", "efek familiar",
                "evcil hayvan etkileri", "yoldas etkileri", "yoldaş etkileri")) {
                inPetEffects = true;
                continue;
            }
            if (LocalizedText.startsWithAny(lower, "minion effects", "effets du serviteur", "efectos de esbirro", "efectos de minion", "efectos del minion", "diener effekte", "effetti servitore", "efeitos do minion", "efekty miniona", "efek minion", "minyon etkileri")) {
                break;
            }
            if (!inPetEffects) {
                continue;
            }

            Identifier texture = textureForPetStat(extractPetStatLabel(line));
            if (texture != null) {
                textures.add(texture);
            }

            if (textures.size() >= maxToAdd) {
                break;
            }
        }
        return textures;
    }

    private static Identifier textureForPetStat(String statName) {
        if (statName == null || statName.isBlank()) {
            return null;
        }
        String canonical = TooltipParsers.canonicalPetStatName(statName);
        if (canonical.isBlank()) {
            canonical = statName;
        }
        String normalized = LocalizedText.normalized(canonical).replaceAll("[^\\p{L}]", "");
        return switch (normalized) {
            case "strength" -> PET_STAT_TEX_STRENGTH;
            case "power" -> PET_STAT_TEX_POWER;
            case "criticalchance" -> PET_STAT_TEX_CRIT_CHANCE;
            case "criticaldamage", "damage" -> PET_STAT_TEX_CRIT_DAMAGE;
            case "defense", "defence" -> PET_STAT_TEX_DEFENSE;
            case "speed" -> PET_STAT_TEX_SPEED;
            case "regeneration", "liferegeneration" -> PET_STAT_TEX_REGEN;
            case "health" -> PET_STAT_TEX_HEALTH;
            case "energy" -> PET_STAT_TEX_ENERGY;
            case "energyregeneration" -> PET_STAT_TEX_ENERGY_REGEN;
            case "dexterity" -> PET_STAT_TEX_DEXTERITY;
            default -> null;
        };
    }

    private static String extractPetStatLabel(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String label = line.replaceFirst("\\s*\\+\\s*[0-9][0-9., \\u00a0]*\\s*%?\\s*$", "");
        label = label.replaceFirst("^[^\\p{L}\\p{N}]*(?:\\(?\\s*(?:LVL|LV|LEVEL|NIVEL)\\s*\\d+\\s*\\)?|S\\.[0-9.]+\\.E)\\s*", "");
        label = label.replaceFirst("^[^\\p{L}\\p{N}]+", "");
        return label.trim();
    }

    public void captureBossFromTooltipLines(ItemStack stack, List<Text> tooltip) {
        if (stack == null || stack.isEmpty() || tooltip == null || tooltip.isEmpty()) {
            return;
        }
        List<String> normalizedLines = TextUtil.normalizeLines(tooltip);
        Optional<TooltipParsers.BossTooltipData> parsed = TooltipParsers.parseBossTooltip(normalizedLines, TextUtil.normalize(stack.getName()));
        if (parsed.isPresent()) {
            TooltipParsers.BossTooltipData data = parsed.get();
            this.bossTracker.captureParsedTooltip(data);
            this.debugLogManager.logInternal(String.format(
                Locale.ROOT,
                "[BOSS] hover parsed: name=%s coords=(%d,%d,%d) remaining=%ds cycle=%dm lines=%s",
                data.bossName(),
                data.x(),
                data.y(),
                data.z(),
                data.remainingSeconds(),
                Math.max(0, data.cycleSeconds() / 60),
                String.join(" | ", normalizedLines)
            ));
            return;
        }

        if (looksLikeBossTooltip(normalizedLines)) {
            this.debugLogManager.logInternal("[BOSS] hover parse miss: " + String.join(" | ", normalizedLines));
        }
    }

    public void savePersistentState() {
        savePersistentState(this.persistentState);
    }

    public String setJobsOverviewVisible(boolean visible) {
        this.config.jobsOverviewVisible = visible;
        this.configManager.saveConfig(this.config);
        return visible ? tr("cmd.jobs.table_shown") : tr("cmd.jobs.table_hidden");
    }

    public String setAllFeaturesVisible(boolean visible) {
        this.config.allFeaturesVisible = visible;
        this.configManager.saveConfig(this.config);
        return visible ? tr("cmd.mod.all_features_shown") : tr("cmd.mod.all_features_hidden");
    }

    public boolean isBossTrackingEnabled() {
        return this.config.bossTrackingEnabled == null || this.config.bossTrackingEnabled;
    }

    public boolean isMinibossHudEnabled() {
        return this.config.minibossHudEnabled == null || this.config.minibossHudEnabled;
    }

    public boolean isModEnabled() {
        return this.config.modEnabled;
    }

    public void setModEnabled(boolean enabled) {
        this.config.modEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setMinibossTrackingEnabled(boolean enabled) {
        this.config.minibossTrackingEnabled = enabled;
        this.bossTracker.setMinibossRegistrationEnabled(enabled && isBossTrackingEnabled());
        this.configManager.saveConfig(this.config);
    }

    public void setMinibossHudEnabled(boolean enabled) {
        this.config.minibossHudEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setMoneyTrackingEnabled(boolean enabled) {
        this.config.moneyTrackingEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void runBalanceCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        this.moneyTracker.onCommandSent("balance");
        client.getNetworkHandler().sendChatCommand("balance");
    }

    public void setJobsTrackingEnabled(boolean enabled) {
        this.config.jobsTrackingEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setEventsEnabled(boolean enabled) {
        this.config.eventsEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setProfileStatsEnabled(boolean enabled) {
        this.config.profileStatsEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setAuctionHighlightEnabled(boolean enabled) {
        this.config.auctionHighlightEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setHakiEnabled(boolean enabled) {
        this.config.hakiEnabled = enabled;
        this.configManager.saveConfig(this.config);
    }

    public void setScrollsEnabled(boolean enabled) {
        this.config.scrollsEnabled = enabled;
        this.configManager.saveConfig(this.config);
        if (!enabled) {
            this.scrollTracker.clear();
        }
    }

    public void setInventoryXpHudEnabled(boolean enabled) {
        this.config.inventoryXpHudEnabled = enabled;
        this.configManager.saveConfig(this.config);
        if (!enabled) {
            this.inventoryXpTracker.clear();
        }
    }

    public PersistentState getPersistentState() {
        return this.persistentState;
    }

    public String setBossTrackingEnabled(boolean enabled) {
        this.config.bossTrackingEnabled = enabled;
        this.bossTracker.setMinibossRegistrationEnabled(enabled && this.config.minibossTrackingEnabled);
        this.configManager.saveConfig(this.config);
        return enabled ? tr("cmd.bosses.tracking_enabled") : tr("cmd.bosses.tracking_disabled");
    }

    public void runProfileCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        this.profileStatsTracker.armProfileHoverSync();
        this.progressHudController.onCommandSent("profile");
        client.getNetworkHandler().sendChatCommand("profile");
    }

    public void sendProfileCommand() {
        // Intentionally disabled: /profile must only be run manually by the player.
        this.debugLogManager.logInternal("[STATS] auto-/profile blocked (manual only)");
    }

    public void copyLastDebugLine() {
        this.debugLogManager.copyLastToClipboard(MinecraftClient.getInstance());
    }

    public String setTalkTarget(String targetName) {
        String normalized = targetName == null ? "" : targetName.trim();
        if (normalized.isBlank()) {
            this.talkTarget = "";
            return tr("cmd.talk.disabled");
        }
        this.talkTarget = normalized;
        return String.format(Locale.ROOT, tr("cmd.talk.enabled_for"), this.talkTarget);
    }

    public String clearTalkTarget() {
        if (this.talkTarget.isBlank()) {
            return tr("cmd.talk.already_disabled");
        }
        this.talkTarget = "";
        return tr("cmd.talk.disabled");
    }

    public String getTalkTarget() {
        return this.talkTarget;
    }

    private void handleTooltip(ItemStack stack, List<Text> lines) {
        List<String> normalizedLines = TextUtil.normalizeLines(lines);
        this.debugLogManager.logTooltip(normalizedLines);

        Optional<TooltipParsers.AuctionParseResult> auctionResult =
            TooltipParsers.parseAuctionHighlight(normalizedLines, stack == null ? 1 : stack.getCount());
        auctionResult.ifPresent(result -> {
            String itemKey = buildAuctionItemKey(stack);
            this.auctionHighlighter.update(itemKey, result);
            this.auctionHighlightRenderCache.remove(itemKey);
        });

        if (this.config.modEnabled && (this.config.profileXpHudEnabled || this.config.grindingHudEnabled)) {
            this.progressHudController.captureTooltip(normalizedLines);
        }
        this.tryUpdateStatsFromLines(normalizedLines);
        this.bossTracker.captureTooltip(TextUtil.normalize(stack == null ? null : stack.getName()), normalizedLines);

        List<TooltipParsers.PetStatLine> petLines = TooltipParsers.parsePetRolls(
            normalizedLines,
            this.debugLogManager::logInternal,
            RarityDetector.detect(stack, lines).map(Enum::name).orElse("")
        );
        if (!this.config.allFeaturesVisible) {
            return;
        }
        if (this.config.modEnabled && this.config.auctionHighlightEnabled) {
            auctionResult.ifPresent(result -> AuctionTooltipFormatter.appendPrices(lines, normalizedLines, result, tr("auction.per_item")));
        }
        for (TooltipParsers.PetStatLine petLine : petLines) {
            long rounded = Math.round(petLine.percent());
            Formatting pctColor = petPercentColor(rounded);
            Text originalLine = lines.get(petLine.lineIndex());
            lines.set(
                petLine.lineIndex(),
                originalLine.copy().append(
                    Text.literal(String.format(Locale.ROOT, " (%.0f%%)", (double) rounded)).formatted(pctColor)
                )
            );
        }

    }

    private static Formatting petPercentColor(long percent) {
        if (percent >= 90L) {
            return Formatting.GREEN;
        }
        if (percent >= 75L) {
            return Formatting.YELLOW;
        }
        if (percent >= 50L) {
            return Formatting.GOLD; // orange-like
        }
        return Formatting.RED;
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.inGameHud == null) {
            return;
        }

        if (this.config.modEnabled && this.config.cookingHudEnabled) {
            this.cookingTracker.tick(client.player);
        }
        syncMenuKeybindConfig();
        handleMenuKeyBinding(client);
        while (this.loadoutKeyBinding.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new net.minepiece.qol.ui.AppearanceScreen(this, null, 1));
        }
        showMenuOpenHintOnce(client);
        if (this.config.modEnabled && (this.config.inventoryXpHudEnabled || this.config.profileXpHudEnabled || this.config.grindingHudEnabled)) {
            SafeExecutor.run(this.debugLogManager, "invxp-scan", () -> {
                this.inventoryXpTracker.scanInventory(client.player);
                long now = System.currentTimeMillis();
                this.progressHudController.onItemXp(this.inventoryXpTracker.takeSharedXpGain(), this.inventoryXpTracker.hasXpSource(), now);
                if (this.progressHudController.shouldAlignItemSources(now)) this.inventoryXpTracker.alignSharedXpSources();
            });
        } else {
            this.inventoryXpTracker.clear();
        }
        SafeExecutor.run(this.debugLogManager, "progress-hud", () -> this.progressHudController.tick(client,
            this.config.modEnabled && (this.config.profileXpHudEnabled || this.config.grindingHudEnabled)));

        SafeExecutor.run(this.debugLogManager, "tab-footer", () -> {
            PlayerListHudAccessor accessor = (PlayerListHudAccessor) client.inGameHud.getPlayerListHud();
            Text footer = accessor.minepiece$getFooter();
            String normalizedFooter = TextUtil.normalize(footer);
            if (!normalizedFooter.equals(this.lastTabFooter)) {
                this.lastTabFooter = normalizedFooter;
                this.bossTracker.onTablistFooter(normalizedFooter);
                if (!normalizedFooter.isBlank()) {
                    this.debugLogManager.logTabFooter(normalizedFooter);
                }
            }
        });

        if (this.config.modEnabled && this.config.allFeaturesVisible) {
            if (isBossTrackingEnabled()) {
                SafeExecutor.run(this.debugLogManager, "boss-gui-scan", () -> this.bossGuiAutoScanner.tick(client));
            }
            if (this.config.profileStatsEnabled) {
                SafeExecutor.run(this.debugLogManager, "stats-refresh-tick", () -> this.statsRefreshController.tick(client));
            }
            if (this.config.scrollsEnabled) {
                SafeExecutor.run(this.debugLogManager, "scroll-scan", () -> this.scrollTracker.scanInventory(client.player));
            } else {
                this.scrollTracker.clear();
            }
        } else {
            this.scrollTracker.clear();
        }

        this.bossTracker.tick();
        this.eventCountdownTracker.tick();
    }

    private void showMenuOpenHintOnce(MinecraftClient client) {
        if (this.menuOpenHintShown || client == null || client.player == null || this.openMenuKeyBinding == null) {
            return;
        }
        String keyName = this.openMenuKeyBinding.getBoundKeyLocalizedText().getString();
        String message = String.format(Locale.ROOT, tr("startup.open_menu_hint"), keyName);
        client.inGameHud.getChatHud().addMessage(Text.literal(message));
        this.menuOpenHintShown = true;
    }

    private void handleMenuKeyBinding(MinecraftClient client) {
        if (client == null || this.openMenuKeyBinding == null) {
            return;
        }
        while (this.openMenuKeyBinding.wasPressed()) {
            if (client.currentScreen instanceof MinepieceMenuScreen) {
                client.setScreen(null);
                continue;
            }
            if (client.currentScreen == null) {
                client.setScreen(new MinepieceMenuScreen(this));
            }
        }
    }

    private void syncMenuKeybindConfig() {
        if (this.openMenuKeyBinding == null) {
            return;
        }
        int boundKeyCode = InputUtil.fromTranslationKey(this.openMenuKeyBinding.getBoundKeyTranslationKey()).getCode();
        if (boundKeyCode == this.config.menuKeybindKey) {
            return;
        }
        this.config.menuKeybindKey = boundKeyCode;
        this.configManager.saveConfig(this.config);
    }

    public void openHudLayoutEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        this.hudEditMode = true;
        this.hudEditSelectedPanel = Math.max(1, Math.min(this.hudEditSelectedPanel, getHudPanelCount()));
        if (this.config.hakiHudX < 0 || this.config.hakiHudY < 0) {
            this.config.hakiHudX = resolveHakiHudX(client);
            this.config.hakiHudY = resolveHakiHudY(client);
            this.configManager.saveConfig(this.config);
        }
        client.setScreen(new HudLayoutScreen(this));
    }

    private void tryUpdateStatsFromLines(List<String> normalizedLines) {
        if (!this.config.modEnabled || !this.config.allFeaturesVisible || !this.config.profileStatsEnabled) {
            return;
        }
        TooltipParsers.parseProfileStats(normalizedLines).ifPresent(this::applyProfileStatsIfRelevant);
    }

    private void applyProfileStatsIfRelevant(TooltipParsers.ProfileStatsData statsData) {
        boolean updated = this.profileStatsTracker.consumeProfileHoverSync(statsData);
        if (updated) {
            this.debugLogManager.logInternal(String.format(
                Locale.ROOT,
                "[STATS] synced via /profile hover: power=%.2f strength=%.2f speed=%.2f",
                statsData.power(),
                statsData.strength(),
                statsData.speed()
            ));
            this.statsRefreshController.onProfileStatsSynced();
            return;
        }

        if (!this.statsRefreshController.isAwaitingProfileSync() || !hasCompleteProfileStats(statsData)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int selectedSlot = -1;
        if (client != null && client.player != null) {
            selectedSlot = client.player.getInventory().getSelectedSlot();
        }
        boolean rememberSlot = this.statsRefreshController.shouldRememberCurrentSlotStats();
        this.profileStatsTracker.update(statsData, selectedSlot, rememberSlot);
        this.statsRefreshController.onProfileStatsSynced();
    }

    private static boolean hasCompleteProfileStats(TooltipParsers.ProfileStatsData statsData) {
        return statsData != null
            && statsData.level() != null
            && statsData.health() != null
            && statsData.damage() != null
            && statsData.criticalChance() != null
            && statsData.criticalDamage() != null
            && statsData.power() != null
            && statsData.strength() != null
            && statsData.energy() != null
            && statsData.energyRegeneration() != null
            && statsData.speed() != null
            && statsData.dexterity() != null
            && statsData.defense() != null
            && statsData.regeneration() != null;
    }

    private void onCommandSent(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        this.progressHudController.onCommandSent(command);

        if (this.config.modEnabled && this.config.allFeaturesVisible && this.config.profileStatsEnabled) {
            this.statsRefreshController.onCommandSent(command);
        }

        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int firstSpace = normalized.indexOf(' ');
        String root = firstSpace >= 0 ? normalized.substring(0, firstSpace) : normalized;
        if (this.config.modEnabled
            && this.config.allFeaturesVisible
            && this.config.profileStatsEnabled
            && ("profile".equalsIgnoreCase(root)
            || "profil".equalsIgnoreCase(root)
            || "profilo".equalsIgnoreCase(root))) {
            this.profileStatsTracker.armProfileHoverSync();
            this.debugLogManager.logInternal("[STATS] /profile|/profil|/profilo detected: awaiting hover sync");
        }
    }

    private boolean handleOutgoingChatMessage(String message) {
        if (message == null || message.isBlank() || this.talkTarget.isBlank()) {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return true;
        }

        String trimmed = message.trim();
        if (trimmed.isBlank()) {
            return false;
        }

        client.getNetworkHandler().sendChatCommand("msg " + this.talkTarget + " " + trimmed);
        this.debugLogManager.logInternal("[TALK] redirected chat to /msg " + this.talkTarget);
        return false;
    }

    private Optional<AuctionHighlighter.ActiveHighlight> getAuctionHighlightForSlot(Slot slot) {
        return getAuctionHighlightForSlot(slot, null);
    }

    private Optional<AuctionHighlighter.ActiveHighlight> getAuctionHighlightForSlot(Slot slot, List<Text> tooltipLines) {
        if (slot == null || !slot.hasStack()) {
            return Optional.empty();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Optional.empty();
        }

        try {
            ItemStack stack = slot.getStack();
            String itemKey = buildAuctionItemKey(stack);
            Optional<AuctionHighlighter.ActiveHighlight> cached = this.auctionHighlightRenderCache.get(itemKey);
            if (cached != null) {
                return cached;
            }

            List<Text> tooltip = tooltipLines;
            if (tooltip == null || tooltip.isEmpty()) {
                tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
            }
            List<String> normalizedLines = TextUtil.normalizeLines(tooltip);
            Optional<AuctionHighlighter.ActiveHighlight> parsed =
                TooltipParsers.parseAuctionHighlight(normalizedLines, stack.getCount()).flatMap(this.auctionHighlighter::createHighlight);
            if (parsed.isPresent()) {
                this.auctionHighlighter.update(itemKey, new TooltipParsers.AuctionParseResult(
                    parsed.get().sellingPrice(),
                    parsed.get().averagePrice(),
                    parsed.get().quantity(),
                    parsed.get().unitPrice(),
                    parsed.get().delta(),
                    Math.min(1.0D, Math.max(0.0D, Math.abs(parsed.get().delta()) / 0.5D))
                ));
                putBounded(this.auctionHighlightRenderCache, itemKey, parsed);
                return parsed;
            }
            Optional<AuctionHighlighter.ActiveHighlight> fallback = this.auctionHighlighter.getCachedHighlight(itemKey);
            putBounded(this.auctionHighlightRenderCache, itemKey, fallback);
            return fallback;
        } catch (Throwable throwable) {
            this.debugLogManager.logInternal("Parser failure in ah-slot: "
                + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
            return this.auctionHighlighter.getCachedHighlight(buildAuctionItemKey(slot.getStack()));
        }
    }

    private static <T> void putBounded(Map<String, T> cache, String key, T value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (cache.size() >= SLOT_OVERLAY_CACHE_MAX) {
            cache.clear();
        }
        cache.put(key, value);
    }

    private String buildAuctionItemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()) + "|"
            + TextUtil.normalize(stack.getName()) + "|"
            + stack.getComponents().hashCode() + "|"
            + stack.getCount();
    }

    private void logAuctionHighlight(AuctionHighlighter.ActiveHighlight highlight, Slot slot, int screenX, int screenY) {
        if (this.debugLogManager == null || !this.debugLogManager.isEnabled()) {
            return;
        }
        int absX = screenX + slot.x;
        int absY = screenY + slot.y;
        String line = String.format(
            Locale.ROOT,
            "[AH] sell=%d avg=%d qty=%d unit=%d delta=%.3f color=0x%08X slot=(%d,%d) screen=(%d,%d) abs=(%d,%d)",
            highlight.sellingPrice(),
            highlight.averagePrice(),
            highlight.quantity(),
            highlight.unitPrice(),
            highlight.delta(),
            highlight.argbColor(),
            slot.x,
            slot.y,
            screenX,
            screenY,
            absX,
            absY
        );
        String previous = this.lastAuctionDebugLines.put(slot.id, line);
        if (!line.equals(previous)) {
            this.debugLogManager.logInternal(line);
        }
    }

    private void savePersistentState(PersistentState state) {
        this.configManager.saveState(state);
    }

    private static boolean looksLikeBossTooltip(List<String> lines) {
        return lines.stream().anyMatch(TooltipParsers::isBossInfoLine);
    }
}
