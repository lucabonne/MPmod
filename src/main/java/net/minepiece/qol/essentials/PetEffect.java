package net.minepiece.qol.essentials;

/**
 * A single rolled pet effect parsed from a tooltip line.
 *
 * @param tier  the level milestone the effect unlocks at (5, 10, 15 or 20);
 *              the wiki ranges are LVL-10, so the scaling factor is {@code tier/10}
 * @param stat  which combat stat
 * @param value the displayed (level-scaled) value
 * @param label the server's display name for the stat as it appeared in the tooltip
 *              (e.g. {@code "Health"} or {@code "Vie"}); may be empty but never null
 */
public record PetEffect(int tier, PetStat stat, double value, String label) {}
