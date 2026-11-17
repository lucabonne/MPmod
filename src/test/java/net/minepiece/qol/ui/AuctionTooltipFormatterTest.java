package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minepiece.qol.i18n.UiLocalization;
import net.minepiece.qol.parse.TooltipParsers;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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

    @Test
    void appendsBothUnitPricesPreservesStylesAndDoesNotDuplicate() {
        Component selling = Component.literal("Selling price: 400").withStyle(ChatFormatting.GOLD);
        Component average = Component.literal("Average price: 380").withStyle(ChatFormatting.AQUA);
        Component unrelated = Component.literal("Seller: Player");
        List<Component> lines = new ArrayList<>(List.of(selling, average, unrelated));
        List<String> original = lines.stream().map(Component::getString).toList();
        TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(original, 10).orElseThrow();
        AuctionTooltipFormatter.appendPrices(lines, original, result, "/item");

        assertEquals("Selling price: 400 (40/item) +5.26%", lines.get(0).getString());
        assertEquals("Average price: 380 (38/item)", lines.get(1).getString());
        assertEquals(selling.getStyle(), lines.get(0).getStyle());
        assertEquals(average.getStyle(), lines.get(1).getStyle());
        assertEquals(net.minecraft.network.chat.TextColor.fromRgb(net.minepiece.qol.state.AuctionHighlighter.rgbForDelta(400.0 / 380 - 1)), lines.get(0).getSiblings().getLast().getStyle().getColor());
        assertSame(unrelated, lines.get(2));
        assertEquals("Selling price: 400", selling.getString());

        List<String> decorated = lines.stream().map(Component::getString).toList();
        assertEquals(result, TooltipParsers.parseAuctionHighlight(decorated, 10).orElseThrow());
        AuctionTooltipFormatter.appendPrices(lines, decorated, result, "/item");
        AuctionTooltipFormatter.appendPrices(lines, original, result, "/item");
        assertEquals(decorated, lines.stream().map(Component::getString).toList());
    }

    @Test
    void compactPricesOnBothLinesDoNotDuplicateOrChangeCalculations() {
        List<Component> lines = new ArrayList<>(List.of(
            Component.literal("Selling price: 16M"), Component.literal("Average price: 15M")
        ));
        List<String> original = lines.stream().map(Component::getString).toList();
        var result = TooltipParsers.parseAuctionHighlight(original, 10).orElseThrow();
        AuctionTooltipFormatter.appendPrices(lines, original, result, "/item");
        assertEquals("Selling price: 16M (1.6M/item) +6.67%", lines.get(0).getString());
        assertEquals("Average price: 15M (1.5M/item)", lines.get(1).getString());
        List<String> decorated = lines.stream().map(Component::getString).toList();
        assertEquals(result, TooltipParsers.parseAuctionHighlight(decorated, 10).orElseThrow());
        AuctionTooltipFormatter.appendPrices(lines, decorated, result, "/item");
        assertEquals(decorated, lines.stream().map(Component::getString).toList());
    }

    @Test
    void formatsFractionalPricesAndLocalizedLabels() {
        List<Component> lines = new ArrayList<>(List.of(
            Component.literal("· Prezzo di vendita: 10"), Component.literal("· Prezzo medio: 20")
        ));
        List<String> original = lines.stream().map(Component::getString).toList();
        TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(original, 3).orElseThrow();
        AuctionTooltipFormatter.appendPrices(lines, original, result, UiLocalization.text("it", "auction.per_item"));
        assertEquals("· Prezzo di vendita: 10 (3.33/oggetto) -50%", lines.get(0).getString());
        assertEquals("· Prezzo medio: 20 (6.67/oggetto)", lines.get(1).getString());
    }

    @Test
    void addsOneDealLineNamingTheStep() {
        List<Component> lines = new ArrayList<>(List.of(Component.literal("Selling price: 400")));
        AuctionTooltipFormatter.appendDealLine(lines, -0.62, key -> key);
        AuctionTooltipFormatter.appendDealLine(lines, -0.62, key -> key); // repeated frames add nothing
        assertEquals(2, lines.size());
        assertEquals("■ auction.deal.great · auction.step", lines.get(1).getString());
        List<Component> fair = new ArrayList<>();
        AuctionTooltipFormatter.appendDealLine(fair, 0.03, key -> key);
        assertEquals("■ auction.fair", fair.getFirst().getString());
        List<Component> pricey = new ArrayList<>();
        AuctionTooltipFormatter.appendDealLine(pricey, 1.5, key -> key.equals("auction.step") ? "step %d of %d" : key);
        assertEquals("■ auction.price.way_over · step 10 of 10", pricey.getFirst().getString());
    }

    @Test
    void priceKeyRangesAndHudValueSplitting() {
        assertEquals("5-15%", AuctionPriceKey.range(1));
        assertEquals("85-95%", AuctionPriceKey.range(9));
        assertEquals("95%+", AuctionPriceKey.range(10));
        assertEquals(List.of("Made today", "+84.2K"), List.of(IconGridTooltipComponent.splitValue("Made today: +84.2K")));
        assertEquals(List.of("Next", "Pumpkin Hunt (5:14:55)"), List.of(IconGridTooltipComponent.splitValue("Next: Pumpkin Hunt (5:14:55)")));
        assertEquals(null, IconGridTooltipComponent.splitValue("09:45"));
        assertEquals(null, IconGridTooltipComponent.splitValue("Spawn 3"));
        assertEquals(null, IconGridTooltipComponent.splitValue("Total: "));
    }
}
