package net.minepiece.qol.essentials;

import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;

/** Aure64's squidcore minion fields; read tags directly so SNBT field ordering is irrelevant. */
public final class MinionNbt {
    private static final Pattern FOOD = Pattern.compile("tracks\\.==([A-Z_]+)");
    private static final Pattern VALUE = Pattern.compile("\"squidcore:value\"\\s*:\\s*\"([^\"]+)\"");
    private MinionNbt() { }
    public static Optional<MinionData> parseTag(CompoundTag tag) {
        String prestige = null, current = null, next = null, food = null;
        for (var entry : tag.entrySet()) {
            if (!(entry.getValue() instanceof CompoundTag field)) continue;
            String key = entry.getKey(), value = field.getStringOr("squidcore:value", "");
            if (key.endsWith("progression-minion_level")) prestige = value;
            else if (key.endsWith("progression-minion_xp")) current = value;
            else if (key.endsWith("progression-minion_next-xp")) next = value;
            else if (key.endsWith("minion-food-name")) {
                var m = FOOD.matcher(value); if (m.find()) food = m.group(1);
            }
        }
        return data(prestige, current, next, food);
    }
    public static Optional<MinionData> parse(String snbt) {
        if (snbt == null) return Optional.empty();
        var food = Pattern.compile("minion-food-name.*?tracks\\.==([A-Z_]+)").matcher(snbt);
        return data(field(snbt, "progression-minion_level"), field(snbt, "progression-minion_xp"),
            field(snbt, "progression-minion_next-xp"), food.find() ? food.group(1) : null);
    }
    private static String field(String snbt, String key) {
        var block = Pattern.compile(Pattern.quote(key) + "\"\\s*:\\s*\\{([^{}]*)}").matcher(snbt);
        if (!block.find()) return null;
        var value = VALUE.matcher(block.group(1));
        return value.find() ? value.group(1) : null;
    }
    private static Optional<MinionData> data(String prestige, String current, String next, String food) {
        if (prestige == null || current == null) return Optional.empty();
        try {
            double level = Double.parseDouble(prestige), xp = Double.parseDouble(current), target = next == null ? 0 : Double.parseDouble(next);
            if (level < 1 || level > 10 || level != Math.floor(level) || !Double.isFinite(level)
                || !Double.isFinite(xp) || xp < 0 || !Double.isFinite(target) || target < 0 || level < 10 && target <= 0) return Optional.empty();
            return Optional.of(new MinionData((int)level, xp, target, food));
        } catch (NumberFormatException ignored) { return Optional.empty(); }
    }
}
