package net.minepiece.qol.ui;

import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minepiece.qol.state.AuctionHighlighter;

/** A small legend of the auction price colors, drawn next to the auction house window. */
public final class AuctionPriceKey {
    private static final int GOLD = 0xFFDDBA6A, BG = 0xEE0B1838, BORDER = 0xFF3D78E0, TEXT = 0xFFEAF1FF, DIM = 0xFF89A8DA;
    private static final int ROW = 10, COLUMN = 64, PAD = 6;

    private AuctionPriceKey() {
    }

    static String range(int level) {
        return level >= AuctionHighlighter.MAX_LEVEL ? "95%+" : (5 + (level - 1) * 10) + "-" + (15 + (level - 1) * 10) + "%";
    }

    public static void draw(GuiGraphicsExtractor g, Font font, Function<String, String> tr, int leftPos, int topPos, int imageWidth, int screenWidth) {
        int width = PAD * 2 + COLUMN * 2 + 4;
        int height = 14 + 12 + AuctionHighlighter.MAX_LEVEL * ROW + 14;
        int x = leftPos + imageWidth + 6;
        if (x + width > screenWidth - 2) x = leftPos - width - 6;
        if (x < 2) return; // no room on either side
        int y = Math.max(2, topPos);
        g.fill(x, y, x + width, y + height, BG);
        outline(g, x, y, width, height, BORDER);
        g.fill(x + 1, y + 13, x + width - 1, y + 14, GOLD);
        g.text(font, tr.apply("auction.key_title"), x + PAD, y + 3, 0xFFFFFFFF);
        int columnsY = y + 18;
        g.text(font, tr.apply("auction.key_cheaper"), x + PAD, columnsY, 0xFF6FD39A);
        g.text(font, tr.apply("auction.key_pricier"), x + PAD + COLUMN + 4, columnsY, 0xFFFFB74A);
        for (int level = 1; level <= AuctionHighlighter.MAX_LEVEL; level++) {
            int rowY = columnsY + 12 + (level - 1) * ROW;
            double away = 0.10 * level;
            row(g, font, x + PAD, rowY, AuctionHighlighter.rgbForDelta(-away), range(level));
            row(g, font, x + PAD + COLUMN + 4, rowY, AuctionHighlighter.rgbForDelta(away), range(level));
        }
        g.text(font, font.plainSubstrByWidth(tr.apply("auction.key_fair"), width - PAD * 2), x + PAD, y + height - 11, DIM);
    }

    private static void row(GuiGraphicsExtractor g, Font font, int x, int y, int rgb, String label) {
        g.fill(x, y, x + 12, y + 8, 0xFF050818);
        g.fill(x + 1, y + 1, x + 11, y + 7, 0xFF000000 | rgb);
        g.text(font, label, x + 16, y, TEXT);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
