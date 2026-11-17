package net.minepiece.qol.essentials;

/**
 * Minion progression read from a pet's NBT.
 *
 * @param prestige   current prestige (1..{@link MinionCost#MAX_PRESTIGE})
 * @param currentXp  XP accumulated toward the next prestige
 * @param nextXp     XP required for the next prestige
 * @param foodToken  resource token (e.g. {@code NETHER_WART}); may be null
 */
public record MinionData(int prestige, double currentXp, double nextXp, String foodToken) {

    /** Maps a squidcore resource token to a vanilla item id, e.g. {@code NETHER_WART → minecraft:nether_wart}. */
    public static String tokenToItemId(String token) {
        if (token == null) return null;
        return "minecraft:" + token.toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isMaxed() {
        return prestige >= MinionCost.MAX_PRESTIGE;
    }
}
