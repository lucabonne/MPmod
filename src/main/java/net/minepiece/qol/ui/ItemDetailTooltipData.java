package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.UiSettings;
import net.minepiece.qol.state.CookingTracker;
import net.minepiece.qol.state.RarityDetector;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;

public record ItemDetailTooltipData(List<IconGridTooltipComponent.Row> rows, boolean hasIngredients) implements TooltipData {
    public static ItemDetailTooltipData from(ItemStack stack, MinepieceQolClient mod) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return null;
        var rarity = RarityDetector.detect(stack);
        var ingredients = CookingTracker.parseIngredients(TextUtil.normalizeLines(lore.lines()));
        if (rarity.isEmpty() && ingredients.isEmpty()) return null;
        List<IconGridTooltipComponent.Row> rows = new ArrayList<>();
        int accent = UiSettings.color(mod.getConfig().appearance.accent, 100);
        String name = rarity.isPresent() ? rarity.get().name() : mod.tr("hud.panel.cooking");
        if (rarity.isPresent()) accent = RarityBadges.color(rarity.get());
        if (!ingredients.isEmpty()) name += " · ×" + mod.getConfig().cookingQuantity;
        rows.add(new IconGridTooltipComponent.Row(name, accent, stack.copy(), null));
        int quantity = mod.getConfig().cookingQuantity;
        for (var entry : ingredients.entrySet()) {
            ItemStack item = HudIcons.findInventoryItem(entry.getKey());
            int owned = 0;
            var player = net.minecraft.client.MinecraftClient.getInstance().player;
            if (player != null) {
                for (int i = 0; i < player.getInventory().size(); i++) {
                    ItemStack candidate = player.getInventory().getStack(i);
                    if (!candidate.isEmpty() && TextUtil.normalize(candidate.getName()).equalsIgnoreCase(entry.getKey())) owned += candidate.getCount();
                }
            }
            int needed = entry.getValue() * quantity;
            rows.add(new IconGridTooltipComponent.Row(entry.getKey() + " (" + owned + "/" + needed + ")",
                owned >= needed ? 0xFF80E082 : UiSettings.color(mod.getConfig().appearance.text, 100), item, null));
        }
        return new ItemDetailTooltipData(List.copyOf(rows), !ingredients.isEmpty());
    }
    public static void removeReplacedLines(List<net.minecraft.text.Text> lines, boolean hasIngredients) {
        boolean inIngredients = false;
        var iterator = lines.listIterator();
        if (iterator.hasNext()) iterator.next(); // The first row is always the item name.
        while (iterator.hasNext()) {
            String line = TextUtil.normalize(iterator.next());
            if (RarityDetector.isBadgeLine(line)) {
                iterator.remove();
            } else if (hasIngredients && CookingTracker.isIngredientHeader(line)) {
                inIngredients = true;
                iterator.remove();
            } else if (inIngredients && CookingTracker.isIngredientLine(line)) {
                iterator.remove();
            } else if (!line.isBlank()) {
                inIngredients = false;
            }
        }
    }

}
