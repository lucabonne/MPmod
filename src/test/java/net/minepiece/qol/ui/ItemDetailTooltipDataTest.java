package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemDetailTooltipDataTest {
    @Test void removesOnlyRowsReplacedByTheGridAndPreservesInstructionsAndPrices() {
        List<Component> lines = new ArrayList<>(List.of(Component.literal("Dish"), Component.literal("Obtaining"),
            Component.literal("An epic reward for cooking"), Component.literal("EPIC"), Component.literal("Ingredients"),
            Component.literal("• Rice (0/1)"), Component.literal("• Bell Pepper (0/1)"), Component.literal("Selling price: 100")));
        ItemDetailTooltipData.removeReplacedLines(lines, true);
        assertEquals(List.of("Dish", "Obtaining", "An epic reward for cooking", "Selling price: 100"),
            lines.stream().map(Component::getString).toList());
        ItemDetailTooltipData.removeReplacedLines(lines, true);
        assertEquals(4, lines.size());
    }

    @Test void keepsUnparsedIngredientSections() {
        List<Component> lines = new ArrayList<>(List.of(Component.literal("Ingredients"), Component.literal("unknown format")));
        ItemDetailTooltipData.removeReplacedLines(lines, false);
        assertEquals(2, lines.size());
    }
}
