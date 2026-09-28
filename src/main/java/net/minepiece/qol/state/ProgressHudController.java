package net.minepiece.qol.state;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minepiece.qol.mixin.BossBarHudAccessor;
import net.minepiece.qol.parse.ProfileXpParser;
import net.minepiece.qol.parse.TooltipParsers;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;

public final class ProgressHudController {
    private final ProfileXpTracker profile = new ProfileXpTracker();
    private final GrindingTracker grinding = new GrindingTracker();
    private long profileRequestUntilMs;
    private long nextScanMs;
    private Screen profileScreen;
    private UUID xpBarId;
    private boolean itemXpAvailable;
    private PersistentState state;
    private Runnable saveState;
    private String identity = "";
    private long nextSaveMs;
    private double pendingItemGain;
    private long pendingItemMs;
    private ProfileXpParser.BarProgress observedBar;
    private ProfileXpParser.BarProgress reconciledBar;
    private long barChangedMs;
    private double combatXpSinceBar;

    public void attachPersistence(PersistentState state, Runnable saveState) {
        this.state = state;
        this.saveState = saveState;
        if (state.playerProgress == null) state.playerProgress = new java.util.LinkedHashMap<>();
    }

    public void connect(String identity, long now) {
        reset();
        this.identity = identity;
        this.grinding.restore(null, now);
        if (this.state != null) {
            var record = this.state.playerProgress.get(identity);
            if (record != null) {
                this.profile.restore(record.profile);
                this.grinding.restore(record.grinding, now);
            }
        }
        this.grinding.tick(now);
        this.grinding.setProfileBaseline(this.profile.getGainedXp());
    }

    public void persist(long now) {
        if (this.state == null || this.identity.isBlank()) return;
        var record = new PersistentState.ProgressRecord();
        record.profile = this.profile.snapshot();
        record.grinding = this.grinding.snapshot(now);
        this.state.playerProgress.put(this.identity, record);
        if (this.saveState != null) this.saveState.run();
        this.nextSaveMs = now + 5_000;
    }

    public void disconnect(long now) {
        persist(now);
        this.identity = "";
        reset();
    }

    public void onItemXp(double gain, boolean available, long now) {
        this.itemXpAvailable = available;
        if (now - this.pendingItemMs > 1000) this.pendingItemGain = 0;
        this.grinding.tick(now);
        if (gain <= 0) return;
        if (this.grinding.hasRecentDanger(now)) {
            applyItemGain(gain, now);
        } else {
            // Inventory and actionbar packets can arrive in either order.
            this.pendingItemGain = now - this.pendingItemMs <= 1000 ? this.pendingItemGain + gain : gain;
            this.pendingItemMs = now;
        }
    }

    public boolean shouldAlignItemSources(long now) {
        return this.pendingItemGain <= 0 && !this.grinding.hasRecentDanger(now);
    }

    private void applyItemGain(double gain, long now) {
        this.profile.addSharedXp(gain);
        this.grinding.onXpGain(gain, now);
        this.combatXpSinceBar += gain;
        this.grinding.setProfileBaseline(this.profile.getGainedXp());
    }

    public ProfileXpTracker profile() {
        return this.profile;
    }

    public GrindingTracker grinding() {
        return this.grinding;
    }

    public void reset() {
        this.profile.reset();
        this.grinding.reset();
        this.profileRequestUntilMs = 0;
        this.nextScanMs = 0;
        this.profileScreen = null;
        this.xpBarId = null;
        this.itemXpAvailable = false;
        this.pendingItemGain = 0;
        this.pendingItemMs = 0;
        this.observedBar = null;
        this.reconciledBar = null;
        this.combatXpSinceBar = 0;
    }

    public void onCommandSent(String command) {
        if (command.trim().matches("(?i)/?(profile|profil|profilo)")) {
            this.profileRequestUntilMs = System.currentTimeMillis() + 15_000L;
            this.profileScreen = null;
        }
    }

    public void captureTooltip(List<String> lines) {
        if (this.profileScreen == null && System.currentTimeMillis() > this.profileRequestUntilMs) {
            return;
        }
        TooltipParsers.ProfileStatsData stats = TooltipParsers.parseProfileStats(lines).orElse(null);
        if (stats == null || stats.health() == null || stats.strength() == null || stats.defense() == null) {
            return;
        }
        ProfileXpParser.parseTooltip(lines).ifPresent(snapshot -> {
            if (this.profile.needsSync() || !snapshot.equals(this.profile.getAnchor())) {
                this.profile.sync(snapshot);
                this.observedBar = null;
                this.reconciledBar = null;
                this.combatXpSinceBar = 0;
                this.grinding.setProfileBaseline(this.profile.getGainedXp());
                persist(System.currentTimeMillis());
            }
        });
    }

    public void onActionbar(String text, long now) {
        this.grinding.onActionbar(text, now);
        if (this.pendingItemGain > 0 && now - this.pendingItemMs <= 1000 && this.grinding.hasRecentDanger(now)) {
            applyItemGain(this.pendingItemGain, now);
            this.pendingItemGain = 0;
        } else if (now - this.pendingItemMs > 1000) {
            this.pendingItemGain = 0;
        }
    }

    public void tick(MinecraftClient client, boolean enabled) {
        long now = System.currentTimeMillis();
        if (this.profileScreen != null && client.currentScreen != this.profileScreen) {
            this.profileScreen = null;
            this.profileRequestUntilMs = 0;
        }
        this.grinding.tick(now);
        if (now >= this.nextSaveMs) persist(now);
        if (!enabled) {
            this.grinding.pause(now);
            return;
        }
        if (this.profileScreen == null && now <= this.profileRequestUntilMs
            && client.currentScreen instanceof HandledScreen<?>) {
            this.profileScreen = client.currentScreen;
        }
        if (this.profileScreen instanceof HandledScreen<?> screen && now >= this.nextScanMs) {
            this.nextScanMs = now + 500L;
            for (var slot : screen.getScreenHandler().slots) {
                if (slot.inventory == client.player.getInventory() || !slot.hasStack()) {
                    continue;
                }
                captureTooltip(TextUtil.normalizeLines(slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC)));
            }
        }
        updateXpBar(((BossBarHudAccessor) client.inGameHud.getBossBarHud()).minepiece$getBossBars(), now);
        this.grinding.observeProfileXp(this.profile.getGainedXp(), now);
    }

    private void updateXpBar(Map<UUID, ClientBossBar> bars, long now) {
        if (this.profile.getAnchor() == null) {
            return;
        }
        if (this.xpBarId != null && !bars.containsKey(this.xpBarId)) {
            this.xpBarId = null;
        }
        if (this.xpBarId == null) {
            UUID candidate = null;
            for (var entry : bars.entrySet()) {
                ProfileXpParser.BarProgress progress = ProfileXpParser.parseBar(TextUtil.normalize(entry.getValue().getName())).orElse(null);
                if (progress == null || !(matchesProfileBar(progress, this.profile.getAnchor())
                    || progress.xpLabel() || progress.level() != null && this.profile.isCached())) {
                    continue;
                }
                if (candidate != null) {
                    return; // Multiple possible bars: wait for an unambiguous profile match.
                }
                candidate = entry.getKey();
            }
            this.xpBarId = candidate;
        }
        if (this.xpBarId != null) {
            ProfileXpParser.parseBar(TextUtil.normalize(bars.get(this.xpBarId).getName()))
                .ifPresent(progress -> observeXpBar(progress, now));
        }
    }

    void observeXpBar(ProfileXpParser.BarProgress progress, long now) {
        if (!progress.equals(this.observedBar)) {
            if (this.observedBar == null || this.observedBar.equals(this.reconciledBar)) this.barChangedMs = now;
            this.observedBar = progress;
        }
        // Let inventory/actionbar packets catch up, and never reapply the same rounded reading each frame.
        if (progress.equals(this.reconciledBar) || now - this.barChangedMs < 500) return;
        boolean wasCached = this.profile.isCached();
        double before = this.profile.getCurrentXp();
        this.profile.reconcilePercent(progress);
        double correction = this.profile.getCurrentXp() - before;
        if (!wasCached && this.combatXpSinceBar > 0) {
            this.grinding.correctXp(Math.max(-this.combatXpSinceBar, correction), now);
        } else if (!wasCached && !this.itemXpAvailable) {
            this.grinding.observeProfileXp(this.profile.getGainedXp(), now);
        }
        this.grinding.setProfileBaseline(this.profile.getGainedXp());
        this.combatXpSinceBar = 0;
        this.reconciledBar = progress;
    }

    static boolean matchesProfileBar(ProfileXpParser.BarProgress progress, ProfileXpParser.Snapshot profile) {
        return (progress.level() == null || progress.level() == profile.level())
            && (progress.xpLabel() || progress.level() != null || Math.abs(progress.percent() - profile.percent()) <= 0.011D);
    }
}
