package net.minepiece.qol.ui;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Colors the value of a "Label: value" HUD row by meaning: green in, red out, amber waiting, grey unknown. */
public final class HudValueColors {
    public static final int GREEN = 0xFF6FE39A, RED = 0xFFFF7A70, AMBER = 0xFFFFC857, DIM = 0xFF7E8AA0;
    private static final List<String> INCOME = List.of("money.made_today", "money.ah_sold", "jobs.today_xp", "jobs.today_money",
        "grinding.money", "progress.gained", "grinding.money_hour");
    private static final List<String> SPENDING = List.of("money.ah_bought");

    private HudValueColors() {
    }

    /** The value's color, or {@code fallback} when the row has no special meaning. */
    public static int color(String label, String value, Function<String, String> tr, int fallback) {
        String v = value.trim();
        String line = label + ": " + value;
        if (line.equals(tr.apply("grinding.active"))) return GREEN;
        if (line.equals(tr.apply("grinding.paused"))) return AMBER;
        if (v.equals(tr.apply("safes.ready"))) return GREEN;
        if (v.equals(tr.apply("safes.picked"))) return DIM;
        if (v.equals("?") || v.equals("?/?") || v.equals("—") || v.equals("-")) return DIM;
        boolean income = matches(label, INCOME, tr) || label.trim().equalsIgnoreCase("XP/h");
        boolean spending = matches(label, SPENDING, tr);
        if (income || spending) {
            if (v.replace("~", "").matches("0+([.,]0+)?")) return DIM; // nothing earned or spent yet
            return income ? GREEN : RED;
        }
        if (v.matches("\\+\\s*[0-9~].*")) return GREEN;
        if (v.matches("[-−]\\s*[0-9].*")) return RED;
        return fallback;
    }

    private static boolean matches(String label, List<String> keys, Function<String, String> tr) {
        String wanted = clean(label);
        for (String key : keys) if (clean(tr.apply(key)).equals(wanted)) return true;
        return false;
    }

    private static String clean(String text) {
        String t = text.trim();
        if (t.endsWith(":")) t = t.substring(0, t.length() - 1).trim();
        return t.toLowerCase(Locale.ROOT);
    }
}
