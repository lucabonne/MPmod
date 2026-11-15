package net.minepiece.qol.essentials;

import java.util.List;

/** The 11 pet combat stats covered by the wiki LVL-10 reference table. */
public enum PetStat {
    VITALITY("Vie", 0xFFE95353),
    STRENGTH("Force", 0xFFE0584C),
    CRIT_CHANCE("Chance Critique", 0xFFE67E4D),
    CRIT_DAMAGE("Dégâts Critiques", 0xFFD67343),
    POWER("Puissance", 0xFFF1C84B),
    ENERGY("Énergie", 0xFFF0B43D),
    ENERGY_REGEN("Régén. Énergie", 0xFFE8A33D),
    SPEED("Vitesse", 0xFF4FC3E8),
    DEXTERITY("Dextérité", 0xFF54C8C6),
    DEFENSE("Défense", 0xFF56A6D0),
    REGEN("Régén.", 0xFFB07FE0);

    private final String fr;
    private final int color;

    PetStat(String fr, int color) {
        this.fr = fr;
        this.color = color;
    }

    /** French display name. */
    public String fr() {
        return fr;
    }

    /** In-game ARGB colour for this stat. */
    public int color() {
        return color;
    }

    /**
     * Aliases as written in the in-game tooltip (FR + EN), ordered most-specific
     * first so that e.g. "energy regen" / "Régénération Énergie" matches
     * {@link #ENERGY_REGEN} before "energy"/"Énergie"/"Régénération".
     */
    private record Alias(String text, PetStat stat) {}

    private static final List<Alias> ALIASES = List.of(
        new Alias("energy regen", ENERGY_REGEN),
        new Alias("régénération énergie", ENERGY_REGEN),
        new Alias("critical chance", CRIT_CHANCE),
        new Alias("chance critique", CRIT_CHANCE),
        new Alias("critical damage", CRIT_DAMAGE),
        new Alias("dégâts critiques", CRIT_DAMAGE),
        new Alias("dégât critique", CRIT_DAMAGE),
        new Alias("regen", REGEN),
        new Alias("régénération", REGEN),
        new Alias("power", POWER),
        new Alias("puissance", POWER),
        new Alias("dexterity", DEXTERITY),
        new Alias("dextérité", DEXTERITY),
        new Alias("defense", DEFENSE),
        new Alias("défense", DEFENSE),
        new Alias("speed", SPEED),
        new Alias("vitesse", SPEED),
        new Alias("strength", STRENGTH),
        new Alias("force", STRENGTH),
        new Alias("energy", ENERGY),
        new Alias("énergie", ENERGY),
        new Alias("health", VITALITY),
        new Alias("life", VITALITY),
        new Alias("vie", VITALITY)
    );

    /** Finds the stat whose label (FR or EN) appears in {@code text}; null if none. */
    public static PetStat fromLabel(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        for (Alias a : ALIASES) {
            if (lower.contains(a.text())) return a.stat();
        }
        return null;
    }
}
