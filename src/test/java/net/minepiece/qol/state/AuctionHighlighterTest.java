package net.minepiece.qol.state;

import java.util.List;
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

    @Test
    void highlightsStackTotalsAndLeavesEqualPricesUntinted() {
        AuctionHighlighter highlighter = new AuctionHighlighter();
        TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(List.of(
            "Selling price: 400", "Average price: 380"
        ), 10).orElseThrow();
        assertEquals(0xFF0000, highlighter.createHighlight(result).orElseThrow().argbColor() & 0x00FFFFFF);

        TooltipParsers.AuctionParseResult equal = TooltipParsers.parseAuctionHighlight(List.of(
            "Selling price: 380", "Average price: 380"
        ), 10).orElseThrow();
        assertEquals(true, highlighter.createHighlight(equal).isEmpty());
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
