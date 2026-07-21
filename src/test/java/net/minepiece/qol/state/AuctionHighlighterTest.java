package net.minepiece.qol.state;

import net.minepiece.qol.parse.TooltipParsers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionHighlighterTest {
    @Test
    void colorsExtremeCheapDiscountBands() {
        AuctionHighlighter highlighter = new AuctionHighlighter();

        assertRgb(highlighter, -0.49D, 0x00FF00);
        assertRgb(highlighter, -0.50D, 0xAA00FF);
        assertRgb(highlighter, -0.73D, 0xAA00FF);
        assertRgb(highlighter, -0.74D, 0x0000FF);
        assertRgb(highlighter, -0.89D, 0x0000FF);
        assertRgb(highlighter, -0.90D, 0xFFFF00);
        assertRgb(highlighter, -1.25D, 0xFFFF00);
        assertRgb(highlighter, 0.25D, 0xFF0000);
    }

    private static void assertRgb(AuctionHighlighter highlighter, double delta, int expectedRgb) {
        TooltipParsers.AuctionParseResult result = new TooltipParsers.AuctionParseResult(
            100.0D,
            100.0D,
            1,
            100.0D,
            delta,
            Math.min(1.0D, Math.abs(delta) / 0.5D)
        );

        int actualRgb = highlighter.createHighlight(result).orElseThrow().argbColor() & 0x00FFFFFF;

        assertEquals(expectedRgb, actualRgb);
    }
}
