package net.minepiece.qol.state;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minepiece.qol.parse.TooltipParsers;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public final class BossGuiAutoScanner {
    private static final long SCAN_INTERVAL_MS = 500L;

    private final BossTracker bossTracker;
    private final DebugLogManager debugLogManager;
    private final Map<Integer, String> slotSignatures = new HashMap<>();

    private Screen lastScreen;
    private long lastScanMs;

    public BossGuiAutoScanner(BossTracker bossTracker, DebugLogManager debugLogManager) {
        this.bossTracker = bossTracker;
        this.debugLogManager = debugLogManager;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }

        Screen currentScreen = client.currentScreen;
        if (!(currentScreen instanceof HandledScreen<?> handledScreen)) {
            resetScreenState();
            return;
        }

        if (this.bossTracker.getCurrentSpawnId().isBlank()) {
            return;
        }

        boolean screenChanged = currentScreen != this.lastScreen;
        if (screenChanged) {
            this.lastScreen = currentScreen;
            this.slotSignatures.clear();
            this.lastScanMs = 0L;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastScanMs < SCAN_INTERVAL_MS) {
            return;
        }
        this.lastScanMs = now;

        int changedSlotsScanned = 0;
        String screenTitle = TextUtil.normalize(currentScreen.getTitle());

        for (int slotIndex = 0; slotIndex < handledScreen.getScreenHandler().slots.size(); slotIndex++) {
            Slot slot = handledScreen.getScreenHandler().slots.get(slotIndex);
            if (slot == null || !slot.hasStack()) {
                continue;
            }

            ItemStack stack = slot.getStack();
            String signature = buildSignature(stack);
            String previous = this.slotSignatures.put(slotIndex, signature);
            if (signature.equals(previous)) {
                continue;
            }

            changedSlotsScanned++;
            List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
            List<String> normalizedLines = TextUtil.normalizeLines(tooltip);
            if (!containsBossKeywords(normalizedLines)) {
                log(String.format(Locale.ROOT, "[BOSS] skip slot=%d missing coords/timer keywords spawn=%s",
                    slotIndex,
                    this.bossTracker.getCurrentSpawnId()));
                continue;
            }

            Optional<TooltipParsers.BossTooltipData> parsed =
                TooltipParsers.parseBossTooltip(normalizedLines, TextUtil.normalize(stack.getName()));
            if (parsed.isEmpty()) {
                log(String.format(Locale.ROOT, "[BOSS] skip slot=%d incomplete coords/timer data spawn=%s",
                    slotIndex,
                    this.bossTracker.getCurrentSpawnId()));
                continue;
            }

            TooltipParsers.BossTooltipData data = parsed.get();
            this.bossTracker.captureParsedTooltip(data);
            log(String.format(
                Locale.ROOT,
                "[BOSS] parsed title=%s slot=%d name=%s coords=(%d,%d,%d) remaining=%ds cycle=%dm spawn=%s",
                quote(screenTitle),
                slotIndex,
                data.bossName(),
                data.x(),
                data.y(),
                data.z(),
                data.remainingSeconds(),
                data.cycleSeconds() / 60,
                this.bossTracker.getCurrentSpawnId()
            ));
        }

        if (screenChanged || changedSlotsScanned > 0) {
            log(String.format(Locale.ROOT, "[BOSS] scan title=%s changedSlots=%d totalSlots=%d spawn=%s",
                quote(screenTitle),
                changedSlotsScanned,
                handledScreen.getScreenHandler().slots.size(),
                this.bossTracker.getCurrentSpawnId()));
        }
    }

    private void resetScreenState() {
        this.lastScreen = null;
        this.slotSignatures.clear();
        this.lastScanMs = 0L;
    }

    private void log(String message) {
        if (this.debugLogManager != null) {
            this.debugLogManager.logInternal(message);
        }
    }

    private static boolean containsBossKeywords(List<String> lines) {
        boolean hasCoords = false;
        boolean hasRespawn = false;
        for (String line : lines) {
            if (line.contains("Coordonn")) {
                hasCoords = true;
            }
            if (line.contains("Respawn")) {
                hasRespawn = true;
            }
            if (hasCoords && hasRespawn) {
                return true;
            }
        }
        return false;
    }

    private static String buildSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()) + "|" + stack.getCount() + "|" + stack.getComponents().hashCode();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "'") + "\"";
    }
}
