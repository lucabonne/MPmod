package net.minepiece.qol;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minepiece.qol.commands.ModCommands;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.mixin.PlayerListHudAccessor;
import net.minepiece.qol.parse.ChatParsers;
import net.minepiece.qol.parse.TooltipParsers;
import net.minepiece.qol.state.AuctionHighlighter;
import net.minepiece.qol.state.BossTracker;
import net.minepiece.qol.state.CooldownTracker;
import net.minepiece.qol.state.DebugLogManager;
import net.minepiece.qol.state.EventCountdownTracker;
import net.minepiece.qol.state.JobsTracker;
import net.minepiece.qol.state.MoneyTracker;
import net.minepiece.qol.state.PersistentState;
import net.minepiece.qol.state.ProfileStatsTracker;
import net.minepiece.qol.state.StatsRefreshController;
import net.minepiece.qol.ui.HudOverlay;
import net.minepiece.qol.ui.HudLayoutScreen;
import net.minepiece.qol.ui.MinibossWaypointWorldRenderer;
import net.minepiece.qol.util.SafeExecutor;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MinepieceQolClient implements ClientModInitializer {
    private static MinepieceQolClient instance;

    private ConfigManager configManager;
    private ConfigManager.ModConfig config;
    private PersistentState persistentState;

    private DebugLogManager debugLogManager;
    private AuctionHighlighter auctionHighlighter;
    private BossTracker bossTracker;
    private CooldownTracker cooldownTracker;
    private ProfileStatsTracker profileStatsTracker;
    private StatsRefreshController statsRefreshController;
    private JobsTracker jobsTracker;
    private MoneyTracker moneyTracker;
    private EventCountdownTracker eventCountdownTracker;
    private HudOverlay hudOverlay;
    private MinibossWaypointWorldRenderer minibossWaypointWorldRenderer;

    private final Map<Integer, String> lastAuctionDebugLines = new HashMap<>();
    private String lastTabFooter = "";
    private boolean hudEditMode;
    private int hudEditSelectedPanel = 1;
    private final Map<Integer, Boolean> hudKeyLatch = new HashMap<>();

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

        this.debugLogManager = new DebugLogManager(this.configManager.getBaseDir(), this.config.debugEnabled);
        this.auctionHighlighter = new AuctionHighlighter();
        this.bossTracker = new BossTracker(this.persistentState, this::savePersistentState, this.debugLogManager);
        this.cooldownTracker = new CooldownTracker();
        this.profileStatsTracker = new ProfileStatsTracker();
        this.statsRefreshController = new StatsRefreshController(this.profileStatsTracker, this::sendProfileCommand, this.debugLogManager);
        this.jobsTracker = new JobsTracker(this.persistentState, this::savePersistentState, this.debugLogManager);
        this.moneyTracker = new MoneyTracker(this.persistentState, this::savePersistentState, this.configManager.getBaseDir());
        this.eventCountdownTracker = new EventCountdownTracker(this.persistentState, this::savePersistentState);
        this.hudOverlay = new HudOverlay(this);
        this.minibossWaypointWorldRenderer = new MinibossWaypointWorldRenderer(this);

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) ->
            SafeExecutor.run(this.debugLogManager, "tooltip", () -> this.handleTooltip(stack, lines)));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ModCommands.register(dispatcher, this));
        ClientSendMessageEvents.COMMAND.register(command -> {
            this.moneyTracker.onCommandSent(command);
            this.onCommandSent(command);
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> savePersistentState(this.persistentState));
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

    public boolean isHudEditMode() {
        return this.hudEditMode;
    }

    public int getHudEditSelectedPanel() {
        return this.hudEditSelectedPanel;
    }

    public void setHudEditSelectedPanel(int panelId) {
        this.hudEditSelectedPanel = Math.max(1, Math.min(6, panelId));
    }

    public int getHudPanelCount() {
        return 6;
    }

    public String getHudPanelName(int panelId) {
        return switch (panelId) {
            case 1 -> "Jobs";
            case 2 -> "Money";
            case 3 -> "Stats";
            case 4 -> "Bosses";
            case 5 -> "Event Timer";
            case 6 -> "Haki";
            default -> "Unknown";
        };
    }

    public int getHudPanelX(int panelId) {
        return switch (panelId) {
            case 1 -> this.config.jobsHudX;
            case 2 -> this.config.moneyHudX;
            case 3 -> this.config.statsHudX;
            case 4 -> this.config.bossHudX;
            case 5 -> this.config.eventsHudX;
            case 6 -> this.config.hakiHudX;
            default -> 0;
        };
    }

    public int getHudPanelY(int panelId) {
        return switch (panelId) {
            case 1 -> this.config.jobsHudY;
            case 2 -> this.config.moneyHudY;
            case 3 -> this.config.statsHudY;
            case 4 -> this.config.bossHudY;
            case 5 -> this.config.eventsHudY;
            case 6 -> this.config.hakiHudY;
            default -> 0;
        };
    }

    public float getHudPanelScale(int panelId) {
        return switch (panelId) {
            case 1 -> this.config.jobsHudScale;
            case 2 -> this.config.moneyHudScale;
            case 3 -> this.config.statsHudScale;
            case 4 -> this.config.bossHudScale;
            case 5 -> this.config.eventsHudScale;
            case 6 -> this.config.hakiHudScale;
            default -> 1.0F;
        };
    }

    public void setHudPanelLayout(int panelId, int x, int y, float scale) {
        float clampedScale = Math.max(0.6F, Math.min(1.8F, scale));
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
                this.config.eventsHudX = x;
                this.config.eventsHudY = y;
                this.config.eventsHudScale = clampedScale;
            }
            case 6 -> {
                this.config.hakiHudX = x;
                this.config.hakiHudY = y;
                this.config.hakiHudScale = clampedScale;
            }
            default -> {
            }
        }
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

    public void handleChatMessage(Text message) {
        String normalized = TextUtil.normalize(message);
        if (normalized.isBlank()) {
            return;
        }

        this.debugLogManager.logChat(normalized);
        SafeExecutor.run(this.debugLogManager, "chat", () -> {
            this.jobsTracker.captureChatMessage(normalized);
            this.cooldownTracker.onChatMessage(normalized);
            this.moneyTracker.onChatMessage(normalized);
            ChatParsers.parseBossKill(normalized).ifPresent(this.bossTracker::onBossKill);
        });
    }

    public void handleActionbarMessage(Text message) {
        String normalized = TextUtil.normalize(message);
        if (normalized.isBlank()) {
            return;
        }

        this.debugLogManager.logActionbar(normalized);
        SafeExecutor.run(this.debugLogManager, "actionbar", () -> {
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
        if (!this.config.allFeaturesVisible) {
            this.lastAuctionDebugLines.remove(slot.id);
            return;
        }

        Optional<AuctionHighlighter.ActiveHighlight> highlight = getAuctionHighlightForSlot(slot);
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
    }

    public void captureBossFromHoveredSlot(Slot slot) {
        if (slot == null || !slot.hasStack()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        ItemStack stack = slot.getStack();
        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
        captureBossFromTooltipLines(stack, tooltip);
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
        return visible ? "Jobs table shown." : "Jobs table hidden.";
    }

    public String setAllFeaturesVisible(boolean visible) {
        this.config.allFeaturesVisible = visible;
        this.configManager.saveConfig(this.config);
        return visible ? "All mod features shown." : "All mod features hidden.";
    }

    public void sendProfileCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.player.networkHandler == null) {
            return;
        }
        client.player.networkHandler.sendChatCommand("profile");
    }

    public void copyLastDebugLine() {
        this.debugLogManager.copyLastToClipboard(MinecraftClient.getInstance());
    }

    private void handleTooltip(ItemStack stack, List<Text> lines) {
        List<String> normalizedLines = TextUtil.normalizeLines(lines);
        this.debugLogManager.logTooltip(normalizedLines);

        TooltipParsers.parseAuctionHighlight(normalizedLines, stack == null ? 1 : stack.getCount())
            .ifPresent(result -> this.auctionHighlighter.update(buildAuctionItemKey(stack), result));

        this.tryUpdateStatsFromLines(normalizedLines);
        this.bossTracker.captureTooltip(TextUtil.normalize(stack == null ? null : stack.getName()), normalizedLines);

        List<TooltipParsers.PetStatLine> petLines = TooltipParsers.parsePetRolls(
            normalizedLines,
            this.debugLogManager::logInternal
        );
        if (!this.config.allFeaturesVisible) {
            return;
        }
        for (TooltipParsers.PetStatLine petLine : petLines) {
            long rounded = Math.round(petLine.percent());
            Text originalLine = lines.get(petLine.lineIndex());
            lines.set(
                petLine.lineIndex(),
                originalLine.copy().append(Text.literal(String.format(Locale.ROOT, " (%.0f%%)", (double) rounded)))
            );
        }
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.inGameHud == null) {
            return;
        }

        handleHudLayoutEditorHotkey(client);

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

        this.bossTracker.tick();
        this.eventCountdownTracker.tick();
    }

    private void handleHudLayoutEditorHotkey(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }

        if (isEdgePressed(client, GLFW.GLFW_KEY_PERIOD)) {
            if (client.currentScreen instanceof HudLayoutScreen) {
                client.setScreen(null);
                return;
            }
            if (client.currentScreen == null) {
                this.hudEditMode = true;
                this.hudEditSelectedPanel = Math.max(1, Math.min(this.hudEditSelectedPanel, getHudPanelCount()));
                if (this.config.hakiHudX < 0 || this.config.hakiHudY < 0) {
                    this.config.hakiHudX = resolveHakiHudX(client);
                    this.config.hakiHudY = resolveHakiHudY(client);
                    this.configManager.saveConfig(this.config);
                }
                client.setScreen(new HudLayoutScreen(this));
            }
        }
    }

    private boolean isEdgePressed(MinecraftClient client, int keyCode) {
        boolean down = isDown(client, keyCode);
        boolean previous = this.hudKeyLatch.getOrDefault(keyCode, false);
        this.hudKeyLatch.put(keyCode, down);
        return down && !previous;
    }

    private static boolean isDown(MinecraftClient client, int keyCode) {
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), keyCode);
    }

    private void tryUpdateStatsFromLines(List<String> normalizedLines) {
        TooltipParsers.parseProfileStats(normalizedLines).ifPresent(this::applyProfileStatsIfArmed);
    }

    private void applyProfileStatsIfArmed(TooltipParsers.ProfileStatsData statsData) {
        boolean updated = this.profileStatsTracker.consumeProfileHoverSync(statsData);
        if (updated) {
            this.debugLogManager.logInternal(String.format(
                Locale.ROOT,
                "[STATS] synced via /profile hover: power=%.2f strength=%.2f speed=%.2f",
                statsData.power(),
                statsData.strength(),
                statsData.speed()
            ));
        }
    }

    private void onCommandSent(String command) {
        if (command == null || command.isBlank()) {
            return;
        }

        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int firstSpace = normalized.indexOf(' ');
        String root = firstSpace >= 0 ? normalized.substring(0, firstSpace) : normalized;
        if ("profile".equalsIgnoreCase(root)
            || "profil".equalsIgnoreCase(root)
            || "profilo".equalsIgnoreCase(root)) {
            this.profileStatsTracker.armProfileHoverSync();
            this.debugLogManager.logInternal("[STATS] /profile|/profil|/profilo detected: awaiting hover sync");
        }
    }

    private Optional<AuctionHighlighter.ActiveHighlight> getAuctionHighlightForSlot(Slot slot) {
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
            List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
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
                return parsed;
            }
            return this.auctionHighlighter.getCachedHighlight(itemKey);
        } catch (Throwable throwable) {
            this.debugLogManager.logInternal("Parser failure in ah-slot: "
                + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
            return this.auctionHighlighter.getCachedHighlight(buildAuctionItemKey(slot.getStack()));
        }
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
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("respawn") || lower.contains("spawn") || lower.contains("apparition") || lower.contains("coord")) {
                return true;
            }
        }
        return false;
    }
}
