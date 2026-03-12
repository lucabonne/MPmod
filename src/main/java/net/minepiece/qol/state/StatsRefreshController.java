package net.minepiece.qol.state;

import java.util.ArrayDeque;
import java.util.Locale;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class StatsRefreshController {
    private static final long DEBOUNCE_MS = 500L;
    private static final long CALIBRATION_STEP_DELAY_MS = 900L;
    private static final long PROFILE_COOLDOWN_MS = 4_000L;
    private static final long INITIAL_PROFILE_DELAY_MS = 1_500L;
    private static final long HINT_COOLDOWN_MS = 30_000L;
    private static final long HINT_VISIBLE_MS = 5_000L;
    private static final long PROFILE_SYNC_TIMEOUT_MS = 10_000L;
    private static final long PETS_COMMAND_TIMEOUT_MS = 15_000L;
    private static final int HOTBAR_SLOTS = 9;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };
    private static final String LEVEL_UP_FRAGMENT = "your stats have been updated";

    private final ProfileStatsTracker tracker;
    private final Runnable profileCommandSender;
    private final DebugLogManager debugLogManager;

    private boolean primed;
    private boolean initialProfileScheduled;
    private boolean calibrationInProgress;
    private boolean calibrationSampleInFlight;
    private boolean restoreSelectedSlotPending;
    private boolean calibrationRequested;
    private int lastSelectedSlot = -1;
    private int calibrationRestoreSlot = -1;
    private long calibrationRequestAtMs;
    private String lastMainHandFingerprint = "";
    private final String[] lastArmorFingerprints = {"", "", "", ""};
    private final ArrayDeque<Integer> calibrationSlots = new ArrayDeque<>();

    private long scheduledProfileAtMs;
    private long lastProfileRequestMs;
    private long lastHintShownMs;
    private long hintVisibleUntilMs;
    private String hintText = "";
    private boolean awaitingProfileSync;
    private boolean petsCommandPending;
    private boolean petsScreenSeen;
    private long petsCommandExpireAtMs;

    public StatsRefreshController(ProfileStatsTracker tracker, Runnable profileCommandSender, DebugLogManager debugLogManager) {
        this.tracker = tracker;
        this.profileCommandSender = profileCommandSender;
        this.debugLogManager = debugLogManager;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }

        if (!this.initialProfileScheduled) {
            this.initialProfileScheduled = true;
            requestCalibration(INITIAL_PROFILE_DELAY_MS, "join");
        }

        snapshotChanges(client);
        resolvePetsScreenLifecycle(client.currentScreen);
        resolveCalibrationRequest(client);
        resolvePendingAction(client);
        resolveProfileRequestTimeout();
        restoreSelectedSlotIfNeeded(client);
    }

    public void onCommandSent(String command) {
        if (command == null || command.isBlank()) {
            return;
        }

        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int firstSpace = normalized.indexOf(' ');
        String root = firstSpace >= 0 ? normalized.substring(0, firstSpace) : normalized;
        if ("pets".equalsIgnoreCase(root)) {
            this.petsCommandPending = true;
            this.petsScreenSeen = false;
            this.petsCommandExpireAtMs = System.currentTimeMillis() + PETS_COMMAND_TIMEOUT_MS;
            log("pets-command");
        }
    }

    public void onChatMessage(String normalizedChat) {
        if (normalizedChat == null || normalizedChat.isBlank()) {
            return;
        }

        String lower = normalizedChat.toLowerCase(Locale.ROOT);
        if (lower.contains(LEVEL_UP_FRAGMENT)) {
            requestCalibration(DEBOUNCE_MS, "level-up");
        }
    }

    public String getHudHintText() {
        if (System.currentTimeMillis() > this.hintVisibleUntilMs) {
            return "";
        }
        return this.hintText;
    }

    public boolean shouldRememberCurrentSlotStats() {
        return this.calibrationSampleInFlight;
    }

    public boolean isAwaitingProfileSync() {
        return this.awaitingProfileSync;
    }

    public boolean onProfileStatsSynced() {
        boolean shouldClose = this.awaitingProfileSync;
        this.awaitingProfileSync = false;
        this.hintVisibleUntilMs = 0L;
        this.hintText = "";
        if (this.calibrationSampleInFlight) {
            this.calibrationSampleInFlight = false;
            advanceCalibration();
        }
        return shouldClose;
    }

    private void snapshotChanges(MinecraftClient client) {
        PlayerInventory inventory = client.player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        String mainHandFingerprint = fingerprint(inventory.getSelectedStack());
        String[] armorFingerprints = new String[ARMOR_SLOTS.length];
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            armorFingerprints[i] = fingerprint(client.player.getEquippedStack(ARMOR_SLOTS[i]));
        }

        if (!this.primed) {
            this.lastSelectedSlot = selectedSlot;
            this.lastMainHandFingerprint = mainHandFingerprint;
            System.arraycopy(armorFingerprints, 0, this.lastArmorFingerprints, 0, ARMOR_SLOTS.length);
            this.primed = true;
            return;
        }

        if (selectedSlot != this.lastSelectedSlot) {
            this.tracker.onSelectedSlotChanged(selectedSlot);
            this.lastSelectedSlot = selectedSlot;
        }

        if (!mainHandFingerprint.equals(this.lastMainHandFingerprint)) {
            this.lastMainHandFingerprint = mainHandFingerprint;
        }

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            if (!armorFingerprints[i].equals(this.lastArmorFingerprints[i])) {
                this.lastArmorFingerprints[i] = armorFingerprints[i];
            }
        }
    }

    private void resolvePendingAction(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (this.scheduledProfileAtMs == 0L || now < this.scheduledProfileAtMs || this.awaitingProfileSync) {
            return;
        }

        if (!this.calibrationInProgress) {
            this.scheduledProfileAtMs = 0L;
            return;
        }

        long allowedAt = this.lastProfileRequestMs + PROFILE_COOLDOWN_MS;
        if (now < allowedAt) {
            this.scheduledProfileAtMs = allowedAt;
            return;
        }

        Integer targetSlot = this.calibrationSlots.peekFirst();
        if (targetSlot != null) {
            client.player.getInventory().setSelectedSlot(targetSlot);
        }

        this.profileCommandSender.run();
        this.lastProfileRequestMs = now;
        this.awaitingProfileSync = true;
        this.calibrationSampleInFlight = this.calibrationInProgress;
        this.scheduledProfileAtMs = 0L;
        log("auto-/profile");
    }

    private void resolveProfileRequestTimeout() {
        if (!this.awaitingProfileSync) {
            return;
        }

        if (this.tracker.getLastUpdateMs() >= this.lastProfileRequestMs) {
            this.awaitingProfileSync = false;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastProfileRequestMs < PROFILE_SYNC_TIMEOUT_MS) {
            return;
        }

        this.awaitingProfileSync = false;
        if (this.calibrationSampleInFlight) {
            this.calibrationSampleInFlight = false;
            advanceCalibration();
            return;
        }
        showHint("Run /profile if stats stop syncing");
    }

    private void resolvePetsScreenLifecycle(Screen currentScreen) {
        if (!this.petsCommandPending) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now > this.petsCommandExpireAtMs) {
            this.petsCommandPending = false;
            this.petsScreenSeen = false;
            return;
        }

        String title = currentScreen == null ? "" : currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
        boolean looksLikePets = title.contains("pet");
        if (looksLikePets) {
            this.petsScreenSeen = true;
            return;
        }

        if (this.petsScreenSeen) {
            this.petsCommandPending = false;
            this.petsScreenSeen = false;
            requestCalibration(250L, "pets-closed");
        }
    }

    private void resolveCalibrationRequest(MinecraftClient client) {
        if (!this.calibrationRequested || this.calibrationInProgress || this.awaitingProfileSync) {
            return;
        }

        if (System.currentTimeMillis() < this.calibrationRequestAtMs) {
            return;
        }

        this.calibrationRequested = false;
        startCalibration(client);
    }

    private void requestCalibration(long delayMs, String reason) {
        long targetTime = System.currentTimeMillis() + Math.max(0L, delayMs);
        if (!this.calibrationRequested || targetTime < this.calibrationRequestAtMs) {
            this.calibrationRequestAtMs = targetTime;
        }
        this.calibrationRequested = true;
        log("calibration-request " + reason);
    }

    private void scheduleProfile(String reason, long delayMs) {
        long targetTime = System.currentTimeMillis() + Math.max(0L, delayMs);
        if (this.scheduledProfileAtMs == 0L || targetTime < this.scheduledProfileAtMs) {
            this.scheduledProfileAtMs = targetTime;
        }
        log("profile-trigger " + reason);
    }

    private void startCalibration(MinecraftClient client) {
        if (this.calibrationInProgress) {
            return;
        }
        this.calibrationSlots.clear();
        this.calibrationRestoreSlot = client.player.getInventory().getSelectedSlot();
        this.calibrationSlots.add(this.calibrationRestoreSlot);
        for (int slot = 0; slot < HOTBAR_SLOTS; slot++) {
            if (slot != this.calibrationRestoreSlot) {
                this.calibrationSlots.add(slot);
            }
        }
        this.calibrationInProgress = true;
        scheduleProfile("slot-calibration-start", 0L);
    }

    private void advanceCalibration() {
        if (!this.calibrationInProgress) {
            return;
        }

        if (!this.calibrationSlots.isEmpty()) {
            this.calibrationSlots.pollFirst();
        }

        if (this.calibrationSlots.isEmpty()) {
            this.calibrationInProgress = false;
            this.restoreSelectedSlotPending = true;
            return;
        }

        scheduleProfile("slot-calibration", CALIBRATION_STEP_DELAY_MS);
    }

    private void restoreSelectedSlotIfNeeded(MinecraftClient client) {
        if (!this.restoreSelectedSlotPending || this.calibrationRestoreSlot < 0) {
            return;
        }

        client.player.getInventory().setSelectedSlot(this.calibrationRestoreSlot);
        this.tracker.onSelectedSlotChanged(this.calibrationRestoreSlot);
        this.lastSelectedSlot = this.calibrationRestoreSlot;
        this.restoreSelectedSlotPending = false;
    }

    private void showHint(String text) {
        long now = System.currentTimeMillis();
        if (now - this.lastHintShownMs < HINT_COOLDOWN_MS) {
            return;
        }
        this.hintText = text;
        this.hintVisibleUntilMs = now + HINT_VISIBLE_MS;
        this.lastHintShownMs = now;
        log("stats-hint " + text);
    }

    private void log(String message) {
        if (this.debugLogManager != null) {
            this.debugLogManager.logInternal("[STATS] " + message);
        }
    }

    private static String fingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()) + "|"
            + TextUtil.normalize(stack.getName());
    }
}
