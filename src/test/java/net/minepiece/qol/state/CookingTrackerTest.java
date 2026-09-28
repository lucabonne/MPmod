package net.minepiece.qol.state;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CookingTrackerTest {
    private CookingTracker recipe() {
        CookingTracker tracker = new CookingTracker();
        tracker.captureRecipe("Dressrosa Paella", List.of("Obtaining", "Epic", "Ingredients",
            "• Bell Pepper (0/1)", "• Cooked Fighting Fish (0/1)", "• Rice (0/1)"));
        return tracker;
    }

    @Test void screenshotAndLiveInventoryChanges() {
        CookingTracker tracker = recipe();
        tracker.updateInventory(Map.of("Bell Pepper", 2, "Cooked Fighting Fish", 2, "Rice", 2));
        assertEquals(List.of("Dressrosa Paella ×5", "Bell Pepper (2/5) 40%", "Cooked Fighting Fish (2/5) 40%", "Rice (2/5) 40%"), tracker.getHudLines(k -> k, 5));
        tracker.updateInventory(Map.of("Bell Pepper", 3));
        assertEquals("Bell Pepper (3/5) 60%", tracker.getHudLines(k -> k, 5).get(1));
        assertEquals("Rice (0/5) 0%", tracker.getHudLines(k -> k, 5).get(3));
    }

    @Test void quantityMultipliesPerDishAndCapsProgress() {
        CookingTracker tracker = new CookingTracker();
        tracker.captureRecipe("Dish", List.of("Ingredients:", "- Rice (99/3)"));
        tracker.updateInventory(Map.of("Rice", 250));
        assertEquals("Rice (250/192) 100%", tracker.getHudLines(k -> k, 64).get(1));
        assertEquals("Rice (250/3) 100%", tracker.getHudLines(k -> k, 1).get(1));
    }

    @Test void formattedNamesAggregateButDoNotPartiallyMatch() {
        CookingTracker tracker = recipe();
        tracker.updateInventory(Map.of("§aBell Pepper", 2, "bell pepper", 1, "Golden Bell Pepper", 12));
        assertEquals("Bell Pepper (3/5) 60%", tracker.getHudLines(k -> k, 5).get(1));
    }

    @Test void repeatedHoverAndUnrelatedTooltipsKeepSelection() {
        CookingTracker tracker = recipe();
        tracker.captureRecipe("Other item", List.of("Rice (0/5)"));
        tracker.captureRecipe("Invalid", List.of("Ingredients", "Rice (0/0)"));
        tracker.captureRecipe("Invalid", List.of("Ingredients", "Rice (0/9999999999999)"));
        assertEquals("Dressrosa Paella ×1", tracker.getHudLines(k -> k, 1).getFirst());
        tracker.captureRecipe("New dish", List.of("Ingredients", "Rice (0/2)", "Usage", "Not an ingredient (0/8)"));
        assertEquals(2, tracker.getHudLines(k -> k, 1).size());
        tracker.clear();
        assertEquals(List.of("cooking.hover_hint"), tracker.getHudLines(k -> k, 1));
    }

    @Test void repeatedRecipeDoesNotDuplicateIngredients() {
        CookingTracker tracker = recipe();
        tracker.updateInventory(Map.of("Rice", 2));
        for (int i = 0; i < 3; i++) {
            tracker.captureRecipe("Dressrosa Paella", List.of("Ingredients", "• Bell Pepper (0/1)",
                "• Cooked Fighting Fish (0/1)", "• Rice (0/1)"));
        }
        assertEquals(4, tracker.getHudLines(k -> k, 5).size());
        assertEquals("Rice (2/5) 40%", tracker.getHudLines(k -> k, 5).get(3));
    }

    @Test void quantityBoundsAndLegacyConfigDefaults() {
        CookingTracker tracker = recipe();
        assertEquals("Dressrosa Paella ×1", tracker.getHudLines(k -> k, 0).getFirst());
        assertEquals("Dressrosa Paella ×64", tracker.getHudLines(k -> k, 100).getFirst());
        var config = new com.google.gson.Gson().fromJson("{}", net.minepiece.qol.config.ConfigManager.ModConfig.class);
        assertTrue(config.cookingHudEnabled);
        assertEquals(1, config.cookingQuantity);
        assertEquals(1.0F, config.cookingHudScale);
    }

    @Test void localizedHeaders() {
        for (String header : List.of("Ingredients", "Ingrédients", "Ingredientes", "Zutaten", "Ingredienti", "Składniki", "Bahan-bahan", "Malzemeler")) {
            CookingTracker tracker = new CookingTracker();
            tracker.captureRecipe("Dish", List.of(header, "• Rice (0/1)"));
            assertEquals(2, tracker.getHudLines(k -> k, 1).size(), header);
        }
    }
}
