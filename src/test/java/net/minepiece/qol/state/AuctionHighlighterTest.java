package net.minepiece.qol.state;

import java.util.List;
import net.minepiece.qol.parse.TooltipParsers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionHighlighterTest {
    @Test
    void stepsEveryTenPercentAfterAFivePercentDeadZone() {
        assertEquals(0, AuctionHighlighter.level(0.0499));
        assertEquals(0, AuctionHighlighter.level(-0.0499));
        assertEquals(1, AuctionHighlighter.level(0.05));
        assertEquals(1, AuctionHighlighter.level(-0.1499));
        assertEquals(2, AuctionHighlighter.level(-0.15));
        assertEquals(5, AuctionHighlighter.level(0.50));
        assertEquals(9, AuctionHighlighter.level(-0.94));
        assertEquals(10, AuctionHighlighter.level(-0.95));
        assertEquals(10, AuctionHighlighter.level(4.0));
    }

    @Test
    void cheapGoesPaleToDeepGreenAndPriceyPaleYellowToCrimson() {
        AuctionHighlighter highlighter = new AuctionHighlighter();
        assertEquals(true, highlighter.createHighlight(result(0.03)).isEmpty());
        assertEquals(0xC6FFB3, AuctionHighlighter.rgbForDelta(-0.06));
        assertEquals(0x00995C, AuctionHighlighter.rgbForDelta(-0.99));
        assertEquals(0xFFF2A0, AuctionHighlighter.rgbForDelta(0.06));
        assertEquals(0xB0001E, AuctionHighlighter.rgbForDelta(2.0));
        int faint = highlighter.createHighlight(result(-0.06)).orElseThrow().argbColor() >>> 24;
        int middle = highlighter.createHighlight(result(-0.50)).orElseThrow().argbColor() >>> 24;
        int strong = highlighter.createHighlight(result(-0.99)).orElseThrow().argbColor() >>> 24;
        assertEquals(true, faint < middle && middle < strong);
        // every step is its own color
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (int i = 0; i < 10; i++) colors.add(AuctionHighlighter.rgbForDelta(-0.10 - i * 0.10));
        assertEquals(10, colors.size());
    }

    @Test
    void highlightsStackTotalsAndLeavesEqualPricesUntinted() {
        AuctionHighlighter highlighter = new AuctionHighlighter();
        TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(List.of(
            "Selling price: 400", "Average price: 380"
        ), 10).orElseThrow();
        assertEquals(AuctionHighlighter.rgbForDelta(0.0526), highlighter.createHighlight(result).orElseThrow().argbColor() & 0x00FFFFFF);

        TooltipParsers.AuctionParseResult equal = TooltipParsers.parseAuctionHighlight(List.of(
            "Selling price: 380", "Average price: 380"
        ), 10).orElseThrow();
        assertEquals(true, highlighter.createHighlight(equal).isEmpty());
    }

    private static TooltipParsers.AuctionParseResult result(double delta) {
        return new TooltipParsers.AuctionParseResult(100.0D, 100.0D, 1, 100.0D, delta, Math.min(1.0D, Math.abs(delta) / 0.5D));
    }
}
