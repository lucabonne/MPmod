package net.minepiece.qol.ui;

import java.util.Locale;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.UiSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class ChatChannelOverlay {
    private ChatChannelOverlay() { }

    public static void render(DrawContext context, int inputX, int inputY) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || !mod.getConfig().modEnabled || !mod.getConfig().allFeaturesVisible) return;
        var tracker = mod.getChatChannelTracker();
        var theme = mod.getConfig().appearance;
        var font = MinecraftClient.getInstance().textRenderer;
        String label = mod.tr("chat_channel." + tracker.channel().name().toLowerCase(Locale.ROOT));
        String code = tracker.languageCode();
        if (code.isEmpty() && !tracker.language().isBlank()) label += " · " + tracker.language();
        int x = Math.max(2, inputX - 2), y = Math.max(0, inputY - 17);
        int width = 26 + font.getWidth(label);
        context.fill(x, y, x + width, y + 14, HudOverlay.panelBackground(theme.chatPanelColor, theme));
        for (int i = 0; i < theme.borderWidth; i++) {
            int color = UiSettings.color(theme.border, theme.hudOpacity);
            context.fill(x + i, y + i, x + width - i, y + i + 1, color);
            context.fill(x + i, y + 13 - i, x + width - i, y + 14 - i, color);
            context.fill(x + i, y + i, x + i + 1, y + 14 - i, color);
            context.fill(x + width - i - 1, y + i, x + width - i, y + 14 - i, color);
        }
        drawFlag(context, code, x + 3, y + 2);
        context.drawText(font, label, x + 23, y + 3, UiSettings.color(theme.text, 100), theme.shadows);
    }

    // Small pixel flags avoid relying on emoji support in Minecraft's font.
    private static void drawFlag(DrawContext context, String code, int x, int y) {
        for (int py = 0; py < 10; py++) {
            for (int px = 0; px < 16; px++) {
                int color = switch (code) {
                    case "it" -> px < 5 ? 0x009246 : px < 11 ? 0xFFFFFF : 0xCE2B37;
                    case "fr" -> px < 5 ? 0x002395 : px < 11 ? 0xFFFFFF : 0xED2939;
                    case "de" -> py < 3 ? 0x171717 : py < 7 ? 0xDD0000 : 0xFFCE00;
                    case "es" -> py < 3 || py >= 7 ? 0xAA151B : 0xF1BF00;
                    case "pl" -> py < 5 ? 0xFFFFFF : 0xDC143C;
                    case "id" -> py < 5 ? 0xFF0000 : 0xFFFFFF;
                    case "ru" -> py < 3 ? 0xFFFFFF : py < 7 ? 0x0039A6 : 0xD52B1E;
                    case "nl" -> py < 3 ? 0xAE1C28 : py < 7 ? 0xFFFFFF : 0x21468B;
                    case "pt" -> Math.abs(px - 6) + Math.abs(py - 5) <= 2 ? 0xFFCC00 : px < 6 ? 0x006600 : 0xFF0000;
                    case "br" -> (px - 8) * (px - 8) + (py - 5) * (py - 5) <= 5 ? 0x002776
                        : Math.abs(px - 8) + Math.abs(py - 5) * 2 <= 7 ? 0xFFDF00 : 0x009B3A;
                    case "tr" -> ((px - 6) * (px - 6) + (py - 5) * (py - 5) <= 9
                        && (px - 7) * (px - 7) + (py - 4) * (py - 4) > 6)
                        || Math.abs(px - 11) + Math.abs(py - 4) <= 1 ? 0xFFFFFF : 0xE30A17;
                    case "en" -> px == 7 || px == 8 || py == 4 || py == 5 ? 0xC8102E
                        : px == 6 || px == 9 || py == 3 || py == 6 ? 0xFFFFFF
                        : Math.abs(px * 9 - py * 15) < 12 || Math.abs((15 - px) * 9 - py * 15) < 12 ? 0xFFFFFF : 0x012169;
                    default -> px == 0 || px == 15 || py == 0 || py == 9 || px == 7 || py == 4 ? 0xCBD5E1 : 0x475569;
                };
                context.fill(x + px, y + py, x + px + 1, y + py + 1, 0xFF000000 | color);
            }
        }
    }
}
