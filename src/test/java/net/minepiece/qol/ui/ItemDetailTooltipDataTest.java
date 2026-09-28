package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemDetailTooltipDataTest {
    @Test void removesOnlyRowsReplacedByTheGridAndPreservesInstructionsAndPrices() {
        List<Text> lines = new ArrayList<>(List.of(Text.literal("Dish"), Text.literal("Obtaining"),
            Text.literal("An epic reward for cooking"), Text.literal("EPIC"), Text.literal("Ingredients"),
            Text.literal("• Rice (0/1)"), Text.literal("• Bell Pepper (0/1)"), Text.literal("Selling price: 100")));
        ItemDetailTooltipData.removeReplacedLines(lines, true);
        assertEquals(List.of("Dish", "Obtaining", "An epic reward for cooking", "Selling price: 100"),
            lines.stream().map(Text::getString).toList());
        ItemDetailTooltipData.removeReplacedLines(lines, true);
        assertEquals(4, lines.size());
    }

    @Test void keepsUnparsedIngredientSections() {
        List<Text> lines = new ArrayList<>(List.of(Text.literal("Ingredients"), Text.literal("unknown format")));
        ItemDetailTooltipData.removeReplacedLines(lines, false);
        assertEquals(2, lines.size());
    }
}
