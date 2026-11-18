package net.minepiece.qol.state;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * What one play session (join to disconnect) earned. The other trackers only keep daily totals,
 * so this adds up how much each daily counter grew while connected.
 */
public final class SessionTracker {
    /** Saved as-is in {@link PersistentState#lastSession}. */
    public static final class Summary {
        public long startMs;
        public long endMs;
        public double money;
        public double ahSold;
        public double ahBought;
        public double combatXp;
        public double jobXp;
        public int bosses;
        public int safes;
    }

    private Summary current;
    private final Map<String, Double> last = new HashMap<>();
    private String moneyDay;

    public void start(long now) {
        this.current = new Summary();
        this.current.startMs = now;
        this.current.endMs = now;
        this.last.clear();
        this.moneyDay = null;
    }

    /** Closes the session; returns it, or null when none was running. */
    public Summary end(long now) {
        Summary ended = this.current;
        if (ended != null) ended.endMs = now;
        this.current = null;
        return ended;
    }

    public Summary current(long now) {
        if (this.current != null) this.current.endMs = now;
        return this.current;
    }

    /**
     * Reads the daily counters. {@code nonAh} can go down (spending), so a new money day is told by
     * {@code moneyDay}; the other counters only grow, so a drop means their day rolled over.
     */
    public void poll(long now, String moneyDay, double nonAh, double ahMade, double ahSpent, double combatXp, double jobXp) {
        if (this.current == null) return;
        this.current.endMs = now;
        boolean newMoneyDay = this.moneyDay != null && !this.moneyDay.equals(moneyDay);
        this.moneyDay = moneyDay;
        this.current.money += grow("nonAh", nonAh, newMoneyDay);
        double sold = grow("ahMade", ahMade, newMoneyDay);
        double bought = grow("ahSpent", ahSpent, newMoneyDay);
        this.current.ahSold += sold;
        this.current.ahBought += bought;
        this.current.money += sold - bought;
        this.current.combatXp += grow("combatXp", combatXp, false);
        this.current.jobXp += grow("jobXp", jobXp, false);
    }

    private double grow(String key, double value, boolean reset) {
        if (!Double.isFinite(value)) return 0;
        Double previous = this.last.put(key, value);
        if (previous == null) return 0; // first reading is the baseline
        boolean signed = key.equals("nonAh"); // spending lowers it, so only the money day tells a reset
        if (reset) return signed ? value : Math.max(0, value);
        if (!signed && value < previous) return Math.max(0, value);
        return value - previous;
    }

    public void onBossKill() { if (this.current != null) this.current.bosses++; }

    public void onSafePicked() { if (this.current != null) this.current.safes++; }

    public static List<String> lines(Summary s, Function<String, String> tr, String titleKey) {
        long minutes = Math.max(0, (s.endMs - s.startMs) / 60_000L);
        double hours = Math.max(1.0 / 60, minutes / 60.0);
        String time = minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
        return List.of(
            tr.apply(titleKey) + " · " + time,
            tr.apply("session.money") + ": " + signed(s.money) + " (" + signed(s.money / hours) + "/h)",
            tr.apply("session.ah") + ": " + tr.apply("session.sold") + " " + ProfileXpTracker.number(s.ahSold)
                + " · " + tr.apply("session.bought") + " " + ProfileXpTracker.number(s.ahBought),
            tr.apply("session.xp") + ": ~" + ProfileXpTracker.number(s.combatXp) + " · " + tr.apply("session.job_xp") + ": " + ProfileXpTracker.number(s.jobXp),
            tr.apply("session.bosses") + ": " + s.bosses + " · " + tr.apply("session.safes") + ": " + s.safes
        );
    }

    private static String signed(double value) {
        return (value > 0 ? "+" : value < 0 ? "-" : "") + String.format(Locale.ROOT, "%,.0f", Math.abs(value));
    }
}
