package net.minepiece.qol.state;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minepiece.qol.parse.TooltipParsers;
import net.minepiece.qol.util.LocalizedText;
import net.minepiece.qol.util.TextUtil;

public final class BossGuiAutoScanner {
    private static final long SCAN_INTERVAL_MS = 500L;
    private static final String GLOBAL_SPAWN_ID = "__global__";

    private final BossTracker bossTracker;
    private final DebugLogManager debugLogManager;
    private final Map<Integer, String> slotSignatures = new HashMap<>();

    private Screen lastScreen;
    private long lastScanMs;
    private long bossCommandUntilMs;
    private Screen requestedScreen;

    public void onCommandSent(String command) {
        if (command != null && command.trim().matches("(?i)/?boss")) {
            this.bossCommandUntilMs = System.currentTimeMillis() + 10_000;
            this.requestedScreen = null;
        }
    }

    public BossGuiAutoScanner(BossTracker bossTracker, DebugLogManager debugLogManager) {
        this.bossTracker = bossTracker;
        this.debugLogManager = debugLogManager;
    }

    public void tick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }

        Screen currentScreen = client.gui.screen();
        if (!(currentScreen instanceof AbstractContainerScreen<?> handledScreen)) {
            resetScreenState();
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
        if (this.requestedScreen != null && this.requestedScreen != currentScreen) this.requestedScreen = null;
        if (now <= this.bossCommandUntilMs) {
            this.requestedScreen = currentScreen;
            this.bossCommandUntilMs = 0;
        }
        boolean likelyBossSelectionScreen = currentScreen == this.requestedScreen || isLikelyBossSelectionScreen(screenTitle);
        if (!likelyBossSelectionScreen) {
            boolean hasBossCandidate = false;
            for (Slot slot : handledScreen.getMenu().slots) {
                if (slot != null && slot.container != client.player.getInventory() && slot.hasItem() && looksLikeBossCandidateName(TextUtil.normalize(slot.getItem().getHoverName()))) {
                    hasBossCandidate = true;
                    break;
                }
            }
            if (!hasBossCandidate) {
                return;
            }
        }

        for (int slotIndex = 0; slotIndex < handledScreen.getMenu().slots.size(); slotIndex++) {
            Slot slot = handledScreen.getMenu().slots.get(slotIndex);
            if (slot == null || slot.container == client.player.getInventory() || !slot.hasItem()) {
                continue;
            }

            ItemStack stack = slot.getItem();
            String stackName = TextUtil.normalize(stack.getHoverName());
            if (!likelyBossSelectionScreen && !looksLikeBossCandidateName(stackName)) {
                continue;
            }
            String signature = buildSignature(stack);
            String previous = this.slotSignatures.put(slotIndex, signature);
            if (signature.equals(previous)) {
                continue;
            }

            changedSlotsScanned++;
            List<String> normalizedLines = net.minepiece.qol.essentials.ItemText.nameAndLore(stack).stream()
                .map(TextUtil::normalize).toList();

            List<TooltipParsers.BossTooltipData> parsed =
                TooltipParsers.parseBossTooltips(normalizedLines, stackName);
            if (!parsed.isEmpty()) {
                this.bossTracker.captureParsedTooltips(stackName, parsed);
                for (TooltipParsers.BossTooltipData data : parsed) {
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
                        activeOrUnknownSpawn()
                    ));
                }
                continue;
            }

            if (likelyBossSelectionScreen || containsBossKeywords(normalizedLines)) {
                String candidateName = bossCandidateName(stack, normalizedLines);
                if (!candidateName.isBlank() && !isControlOrPlaceholderName(candidateName)) {
                    this.bossTracker.registerEncounteredBoss(candidateName);
                    log(String.format(
                        Locale.ROOT,
                        "[BOSS] registry add title=%s slot=%d name=%s spawn=%s",
                        quote(screenTitle),
                        slotIndex,
                        quote(candidateName),
                        activeOrUnknownSpawn()
                    ));
                    continue;
                }
            }

            log(String.format(Locale.ROOT, "[BOSS] skip slot=%d incomplete boss data spawn=%s",
                slotIndex,
                activeOrUnknownSpawn()));
        }

        if (screenChanged || changedSlotsScanned > 0) {
            log(String.format(Locale.ROOT, "[BOSS] scan title=%s changedSlots=%d totalSlots=%d spawn=%s",
                quote(screenTitle),
                changedSlotsScanned,
                handledScreen.getMenu().slots.size(),
                activeOrUnknownSpawn()));
        }
    }

    private void resetScreenState() {
        this.lastScreen = null;
        this.requestedScreen = null;
        this.slotSignatures.clear();
        this.lastScanMs = 0L;
    }

    private void log(String message) {
        if (this.debugLogManager != null) {
            this.debugLogManager.logInternal(message);
        }
    }

    private String activeOrUnknownSpawn() {
        String spawnId = this.bossTracker.getCurrentSpawnId();
        return spawnId == null || spawnId.isBlank() ? GLOBAL_SPAWN_ID : spawnId;
    }

    private static boolean isLikelyBossSelectionScreen(String screenTitle) {
        if (screenTitle == null || screenTitle.isBlank()) {
            return false;
        }
        String lower = screenTitle.toLowerCase(Locale.ROOT);
        return LocalizedText.containsAny(lower,
            "island", "ile", "île", "isla", "insel", "isola", "ilha", "wyspa", "pulau", "ada",
            "boss",
            "raid", "incursion", "razzia", "najazd", "serangan",
            "marines", "marine", "marina", "marynarka", "denizci",
            "pirates", "piratas", "piraten", "pirati", "piraci", "bajak laut", "korsan");
    }

    private static boolean containsBossKeywords(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (TooltipParsers.isBossInfoLine(line)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeBossCandidateName(String name) {
        return LocalizedText.containsAny(name,
            "boss",
            "raid", "incursion", "razzia", "najazd", "serangan",
            "marine", "marines", "marina", "marynarka", "denizci",
            "pirate", "pirates", "pirata", "piratas", "piraten", "pirati", "piraci", "bajak laut", "korsan");
    }

    private static String bossCandidateName(ItemStack stack, List<String> normalizedLines) {
        if (normalizedLines != null) {
            for (String line : normalizedLines) {
                if (line != null && !line.isBlank()) {
                    return line.trim();
                }
            }
        }
        return TextUtil.normalize(stack == null ? null : stack.getHoverName());
    }

    private static boolean isControlOrPlaceholderName(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return LocalizedText.containsAny(lower,
            "back", "return", "retour", "volver", "zuruck", "zurück", "indietro", "voltar", "wroc", "wróć", "kembali", "geri",
            "close", "fermer", "cerrar", "schliessen", "schließen", "chiudi", "fechar", "zamknij", "tutup", "kapat",
            "next", "suivant", "siguiente", "weiter", "prossimo", "próximo", "nastepny", "następny", "berikutnya", "sonraki",
            "previous", "precedent", "précédent", "anterior", "vorherige", "precedente", "poprzedni", "sebelumnya", "onceki", "önceki",
            "page", "empty", "vide", "vacio", "vacío", "leer", "vuoto", "pusty", "kosong", "bos", "boş")
            || lower.equals("???")
            || lower.equals("-");
    }

    private static String buildSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getCount() + "|" + stack.getComponents().hashCode();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "'") + "\"";
    }
}
