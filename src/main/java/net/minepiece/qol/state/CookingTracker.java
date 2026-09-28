package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class CookingTracker {
    private static final Pattern HEADER = Pattern.compile("(?iu)^(?:[^\\p{L}]*)(?:ingredients|ingrédients|ingredientes|zutaten|ingredienti|składniki|bahan(?:-bahan)?|malzemeler)\\s*:?");
    private static final Pattern INGREDIENT = Pattern.compile("^[•·▪●\\-]?\\s*(.+?)\\s*\\(\\s*\\d+\\s*/\\s*(\\d+)\\s*\\)\\s*$");
    private String recipeName = "";
    private Map<String, Integer> ingredients = Map.of();
    private Map<String, Integer> inventory = Map.of();

    public void captureRecipe(String name, List<String> lines) {
        Map<String, Integer> parsed = parseIngredients(lines);
        if (!parsed.isEmpty()) {
            this.recipeName = TextUtil.normalize(name);
            this.ingredients = parsed;
        }
    }

    public static boolean isIngredientHeader(String line) { return HEADER.matcher(TextUtil.normalize(line)).matches(); }
    public static boolean isIngredientLine(String line) { return INGREDIENT.matcher(TextUtil.normalize(line)).matches(); }

    public static Map<String, Integer> parseIngredients(List<String> lines) {
        Map<String, Integer> parsed = new LinkedHashMap<>();
        boolean inIngredients = false;
        for (String raw : lines) {
            String line = TextUtil.normalize(raw);
            if (!inIngredients) {
                inIngredients = HEADER.matcher(line).matches();
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            var match = INGREDIENT.matcher(line);
            if (!match.matches()) {
                break;
            }
            try {
                int count = Integer.parseInt(match.group(2));
                if (count <= 0 || count > Integer.MAX_VALUE / 64) {
                    return Map.of();
                }
                String ingredient = match.group(1).trim();
                if (parsed.putIfAbsent(ingredient, count) != null) {
                    return Map.of();
                }
            } catch (NumberFormatException ignored) {
                return Map.of();
            }
        }
        return parsed;
    }

    public void tick(PlayerEntity player) {
        if (this.ingredients.isEmpty()) {
            return;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                counts.merge(TextUtil.normalize(stack.getName()), stack.getCount(), Integer::sum);
            }
        }
        updateInventory(counts);
    }

    void updateInventory(Map<String, Integer> counts) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        counts.forEach((name, count) -> normalized.merge(key(name), count, Integer::sum));
        this.inventory = normalized;
    }

    private static String key(String name) {
        return TextUtil.normalize(name).toLowerCase(Locale.ROOT);
    }

    public List<String> getHudLines(Function<String, String> tr, int dishes) {
        if (this.ingredients.isEmpty()) {
            return List.of(tr.apply("cooking.hover_hint"));
        }
        int quantity = Math.max(1, Math.min(64, dishes));
        List<String> lines = new ArrayList<>();
        lines.add(this.recipeName + " ×" + quantity);
        this.ingredients.forEach((name, perDish) -> {
            int needed = perDish * quantity;
            int owned = this.inventory.getOrDefault(key(name), 0);
            long percent = Math.round(Math.min(100.0, owned * 100.0 / needed));
            lines.add(name + " (" + owned + "/" + needed + ") " + percent + "%");
        });
        return lines;
    }

    public void clear() {
        this.recipeName = "";
        this.ingredients = Map.of();
        this.inventory = Map.of();
    }
}
