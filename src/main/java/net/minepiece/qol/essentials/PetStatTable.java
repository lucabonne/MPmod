package net.minepiece.qol.essentials;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The "Stats Pet" reference: the minimum and maximum value a stat can roll for a
 * given rarity, at a given pet level. A roll's quality is its position between
 * those bounds: {@code (value - min) / (max - min)}.
 *
 * <p>The reference table is given at level 10; in game the bounds scale linearly
 * with the pet level (the "LVL X" badge), so {@code bound(level) = boundL10 *
 * level / 10}. This holds for every level (5, 11, 15, 20, …).
 *
 * <p>Each L10 row is ordered to match {@link PetStat#ordinal()}: Vie, Force,
 * Chance Critique, Dégâts Critiques, Puissance, Énergie, Régén énergie, Vitesse,
 * Dextérité, Défense, Régén.
 *
 * <p>Crit damage uses the post-rebalance scale (server "+200% on familiars").
 */
public final class PetStatTable {

    private static final Map<Rarity, double[]> MIN_L10 = new EnumMap<>(Rarity.class);
    private static final Map<Rarity, double[]> MAX_L10 = new EnumMap<>(Rarity.class);

    private PetStatTable() {}

    static {
        // Crit damage (index 3) is the L10 half of the wiki's "lvl 20" table.
        //                            Vie    Force  CritCh CritDmg  Puis   Énerg  EnReg  Vit     Dext    Déf    Regen
        MIN_L10.put(Rarity.COMMON,    new double[]{18.75, 1.875, 0.5,   1.875,  1.875, 18.75, 1.875, 0.9375, 0.9375, 1.875, 1.875});
        MIN_L10.put(Rarity.RARE,      new double[]{25,    2.5,   0.625, 3.75,   2.5,   25,    2.5,   1.25,   1.25,   2.5,   2.5});
        MIN_L10.put(Rarity.EPIC,      new double[]{37.5,  3.75,  1,     5.625,  3.75,  37.5,  3.75,  1.875,  1.875,  3.75,  3.75});
        MIN_L10.put(Rarity.LEGENDARY, new double[]{50,    5,     1.25,  7.5,    5,     50,    5,     2.5,    2.5,    5,     5});
        MIN_L10.put(Rarity.MYTHIC,    new double[]{75,    7.5,   1.875, 9.375,  7.5,   75,    7.5,   3.75,   3.75,   7.5,   7.5});

        //                            Vie    Force  CritCh CritDmg  Puis   Énerg  EnReg  Vit     Dext    Déf    Regen
        MAX_L10.put(Rarity.COMMON,    new double[]{37.5,  3.75,  1,     3.75,   3.75,  37.5,  3.75,  1.875,  1.875,  3.75,  3.75});
        MAX_L10.put(Rarity.RARE,      new double[]{50,    5,     1.25,  7.5,    5,     50,    5,     2.5,    2.5,    5,     5});
        MAX_L10.put(Rarity.EPIC,      new double[]{75,    7.5,   2,     11.25,  7.5,   75,    7.5,   3.75,   3.75,   7.5,   7.5});
        MAX_L10.put(Rarity.LEGENDARY, new double[]{100,   10,    2.5,   15,     10,    100,   10,    5,      5,      10,    10});
        MAX_L10.put(Rarity.MYTHIC,    new double[]{150,   12.5,  3.125, 18.75,  12.5,  150,   12.5,  6.25,   6.25,   12.5,  12.5});
    }

    /** Minimum (worst) roll for the rarity/level/stat, or empty if unknown or level &le; 0. */
    public static OptionalDouble min(Rarity rarity, int level, PetStat stat) {
        return scaled(MIN_L10, rarity, level, stat);
    }

    /** Maximum (best) roll for the rarity/level/stat, or empty if unknown or level &le; 0. */
    public static OptionalDouble max(Rarity rarity, int level, PetStat stat) {
        return scaled(MAX_L10, rarity, level, stat);
    }

    private static OptionalDouble scaled(Map<Rarity, double[]> table, Rarity rarity,
                                         int level, PetStat stat) {
        double[] row = rarity == null ? null : table.get(rarity);
        if (row == null || level <= 0) return OptionalDouble.empty();
        return OptionalDouble.of(row[stat.ordinal()] * level / 10.0);
    }
}
