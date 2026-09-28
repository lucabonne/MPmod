package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minepiece.qol.i18n.UiLocalization;
import net.minepiece.qol.parse.TooltipParsers;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AuctionTooltipFormatterTest {
    @ParameterizedTest
    @CsvSource({
        "0.052631578947, +5.26%", "0.051, +5.1%", "0.05333333333, +5.33%",
        "0.05336, +5.34%", "0.05335, +5.34%", "0.05, +5%",
        "-0.05335, -5.34%", "-0.051, -5.1%", "0.0, 0%",
        "-0.0, 0%", "-0.00001, 0%", "0.00001, 0%"
    })
    void formatsPercentages(double delta, String expected) {
        assertEquals(expected, AuctionTooltipFormatter.formatDeltaPercent(delta));
    }

    @ParameterizedTest
    @CsvSource({"40, 40", "5.1, 5.1", "5.333333, 5.33", "5.335, 5.34", "0, 0",
        "999, 999", "1000, 1K", "1600, 1.6K", "2535, 2.54K",
        "999990, 999.99K", "999999, 1M", "1000000, 1M", "1600000, 1.6M", "1635000, 1.64M"})
    void formatsUnitPrices(double value, String expected) {
        assertEquals(expected, AuctionTooltipFormatter.formatPrice(value));
    }

    @ParameterizedTest
    @CsvSource({
        "-0.49999, GREEN", "-0.50, LIGHT_PURPLE", "-0.73999, LIGHT_PURPLE",
        "-0.74, BLUE", "-0.89999, BLUE", "-0.90, YELLOW",
        "0.052631578947, RED", "0, GRAY"
    })
    void usesUnroundedColorThresholds(double delta, Formatting expected) {
        assertEquals(expected, AuctionTooltipFormatter.deltaColor(delta));
    }

    @Test
    void appendsBothUnitPricesPreservesStylesAndDoesNotDuplicate() {
        Text selling = Text.literal("Selling price: 400").formatted(Formatting.GOLD);
        Text average = Text.literal("Average price: 380").formatted(Formatting.AQUA);
        Text unrelated = Text.literal("Seller: Player");
        List<Text> lines = new ArrayList<>(List.of(selling, average, unrelated));
        List<String> original = lines.stream().map(Text::getString).toList();
        TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(original, 10).orElseThrow();
        AuctionTooltipFormatter.appendPrices(lines, original, result, "/item");

        assertEquals("Selling price: 400 (40/item) +5.26%", lines.get(0).getString());
        assertEquals("Average price: 380 (38/item)", lines.get(1).getString());
        assertEquals(selling.getStyle(), lines.get(0).getStyle());
        assertEquals(average.getStyle(), lines.get(1).getStyle());
        assertEquals(Formatting.RED.getColorValue(), lines.get(0).getSiblings().getLast().getStyle().getColor().getRgb());
        assertSame(unrelated, lines.get(2));
        assertEquals("Selling price: 400", selling.getString());

        List<String> decorated = lines.stream().map(Text::getString).toList();
        assertEquals(result, TooltipParsers.parseAuctionHighlight(decorated, 10).orElseThrow());
        AuctionTooltipFormatter.appendPrices(lines, decorated, result, "/item");
        AuctionTooltipFormatter.appendPrices(lines, original, result, "/item");
        assertEquals(decorated, lines.stream().map(Text::getString).toList());
    }

    @Test
    void compactPricesOnBothLinesDoNotDuplicateOrChangeCalculations() {
        List<Text> lines = new ArrayList<>(List.of(
            Text.literal("Selling price: 16M"), Text.literal("Average price: 15M")
        ));
        List<String> original = lines.stream().map(Text::getString).toList();
        var result = TooltipParsers.parseAuctionHighlight(original, 10).orElseThrow();
        AuctionTooltipFormatter.appendPrices(lines, original, result, "/item");
        assertEquals("Selling price: 16M (1.6M/item) +6.67%", lines.get(0).getString());
        assertEquals("Average price: 15M (1.5M/item)", lines.get(1).getString());
        List<String> decorated = lines.stream().map(Text::getString).toList();
        assertEquals(result, TooltipParsers.parseAuctionHighlight(decorated, 10).orElseThrow());
        AuctionTooltipFormatter.appendPrices(lines, decorated, result, "/item");
        assertEquals(decorated, lines.stream().map(Text::getString).toList());
    }

    @Test
    void formatsFractionalPricesAndLocalizedLabels() {
        List<Text> lines = new ArrayList<>(List.of(
            Text.literal("· Prezzo di vendita: 10"), Text.literal("· Prezzo medio: 20")
        ));
        List<String> original = lines.stream().map(Text::getString).toList();
        TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(original, 3).orElseThrow();
        AuctionTooltipFormatter.appendPrices(lines, original, result, UiLocalization.text("it", "auction.per_item"));
        assertEquals("· Prezzo di vendita: 10 (3.33/oggetto) -50%", lines.get(0).getString());
        assertEquals("· Prezzo medio: 20 (6.67/oggetto)", lines.get(1).getString());
    }
}
