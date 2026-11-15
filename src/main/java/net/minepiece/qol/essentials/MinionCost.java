package net.minepiece.qol.essentials;

/**
 * Minion prestige cost maths.
 *
 * <p>The cost to advance from prestige {@code p} to {@code p+1} is
 * {@code base * 2^(p-1)}, where {@code base} (the prestige-1 cost) depends only
 * on rarity. Rather than hard-code rarity bases, it is derived from the pet's
 * own NBT: {@code base = nextXp / 2^(prestige-1)}. Total to reach the max
 * prestige is therefore {@code base * (2^9 - 1)}.
 */
public final class MinionCost {

    public static final int MAX_PRESTIGE = 10;

    private MinionCost() {}

    /** XP left to reach the next prestige. */
    public static double remainingToNextPrestige(double currentXp, double nextXp) {
        return Math.max(0.0, nextXp - currentXp);
    }

    /** XP left to reach the maximum prestige ({@value #MAX_PRESTIGE}). */
    public static double remainingToMax(int prestige, double currentXp, double nextXp) {
        if (prestige >= MAX_PRESTIGE || prestige < 1 || nextXp <= 0) return 0.0;
        double pow = Math.pow(2, prestige - 1);          // 2^(prestige-1)
        double base = nextXp / pow;                      // prestige-1 cost
        double remaining = base * (512 - pow) - currentXp; // base*(2^9 - 2^(p-1)) - invested
        return Math.max(0.0, remaining);
    }
}
