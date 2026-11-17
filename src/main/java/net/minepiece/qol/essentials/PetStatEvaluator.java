package net.minepiece.qol.essentials;

import java.util.OptionalDouble;

/**
 * Computes a roll's quality in {@code [0, 1]} as its position between the minimum
 * and maximum for the effect's rarity, level and stat:
 * {@code (value - min) / (max - min)} (0 = worst roll, 1 = best roll).
 *
 * <p>Returns empty when the rarity/stat is undefined, or when the value lands far
 * above the max (a server "special" effect that ignores the table).
 */
public final class PetStatEvaluator {

    /** Beyond this fraction of the band above the max, the value is treated as off-table. */
    private static final double OFF_TABLE = 1.0;

    private PetStatEvaluator() {}

    public static OptionalDouble quality(Rarity rarity, PetEffect effect) {
        if (rarity == null || effect == null || !Double.isFinite(effect.value())) return OptionalDouble.empty();

        OptionalDouble minOpt = PetStatTable.min(rarity, effect.tier(), effect.stat());
        OptionalDouble maxOpt = PetStatTable.max(rarity, effect.tier(), effect.stat());
        if (minOpt.isEmpty() || maxOpt.isEmpty()) return OptionalDouble.empty();

        double min = minOpt.getAsDouble();
        double max = maxOpt.getAsDouble();
        double range = max - min;
        if (range <= 0) return OptionalDouble.empty();

        double q = (effect.value() - min) / range;
        // A "special" server roll sits more than one full band above the max.
        if (q > 1.0 + OFF_TABLE) return OptionalDouble.empty();
        return OptionalDouble.of(Math.max(0.0, Math.min(1.0, q)));
    }
}
