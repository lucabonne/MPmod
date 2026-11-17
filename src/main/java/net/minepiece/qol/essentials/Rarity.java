package net.minepiece.qol.essentials;

/** Pet rarity tiers, as named in the squidcore item NBT track tokens. */
public enum Rarity {
    COMMON(0xFFFFFFFF),
    RARE(0xFF5599FF),
    EPIC(0xFFB24BFF),
    LEGENDARY(0xFFFFAA00),
    MYTHIC(0xFFFF5577);

    private final int color;

    Rarity(int color) {
        this.color = color;
    }

    /** ARGB display colour for this rarity. */
    public int color() {
        return color;
    }

    /** Parses a track token such as {@code "LEGENDARY"} (case-insensitive); null if unknown. */
    public static Rarity fromTrack(String token) {
        if (token == null) return null;
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(token.trim())) return r;
        }
        return null;
    }
}
