package net.minepiece.qol.state;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/** The cheapest listing and the server's average price seen per item and day, for "lowest this week" and the weekly trend. */
public final class AuctionPriceMemory {
    /** Saved as-is in {@link PersistentState#auctionPrices}. */
    public static final class Day {
        public String day = "";
        public double low;
        public double avg;
    }

    public record Low(double price, long daysAgo) { }

    private static final int KEEP_DAYS = 14;
    private static final int WEEK_DAYS = 7;
    // shortcut: past this many items the least recently seen one is dropped; raise it if people trade far more items.
    private static final int MAX_ITEMS = 3000;

    private final Map<String, List<Day>> items;
    private final Runnable save;

    public AuctionPriceMemory(Map<String, List<Day>> items, Runnable save) {
        this.items = items;
        this.save = save;
    }

    /** One listing's per-item price and the server's per-item average. Saves only when the day's record changed. */
    public void observe(String item, double unitPrice, double averageUnitPrice, LocalDate today) {
        if (item == null || item.isBlank() || !(unitPrice > 0) || !Double.isFinite(unitPrice)) return;
        String todayId = today.toString();
        List<Day> days = this.items.get(item);
        if (days == null) {
            if (this.items.size() >= MAX_ITEMS) evictOldest();
            days = new ArrayList<>();
            this.items.put(item, days);
        }
        days.removeIf(d -> d == null || age(d, today) > KEEP_DAYS);
        Day current = days.isEmpty() ? null : days.getLast();
        boolean changed = false;
        if (current == null || !todayId.equals(current.day)) {
            current = new Day();
            current.day = todayId;
            current.low = unitPrice;
            days.add(current);
            changed = true;
        } else if (unitPrice < current.low) {
            current.low = unitPrice;
            changed = true;
        }
        if (averageUnitPrice > 0 && Double.isFinite(averageUnitPrice) && current.avg != averageUnitPrice) {
            current.avg = averageUnitPrice;
            changed = true;
        }
        if (changed) this.save.run();
    }

    /** Cheapest listing of the last seven days (today included). */
    public Optional<Low> lowest(String item, LocalDate today) {
        Low best = null;
        for (Day d : this.items.getOrDefault(item, List.of())) {
            long age = age(d, today);
            if (age < 0 || age >= WEEK_DAYS) continue;
            if (best == null || d.low < best.price()) best = new Low(d.low, age);
        }
        return Optional.ofNullable(best);
    }

    /** How the server average moved from the oldest day of the week to the newest, e.g. 0.12 for +12%; needs two days. */
    public OptionalDouble weeklyTrend(String item, LocalDate today) {
        Day oldest = null, newest = null;
        for (Day d : this.items.getOrDefault(item, List.of())) {
            long age = age(d, today);
            if (age < 0 || age >= WEEK_DAYS || !(d.avg > 0)) continue;
            if (oldest == null || age > age(oldest, today)) oldest = d;
            if (newest == null || age < age(newest, today)) newest = d;
        }
        if (oldest == null || oldest == newest) return OptionalDouble.empty();
        return OptionalDouble.of(newest.avg / oldest.avg - 1);
    }

    private void evictOldest() {
        String oldestItem = null;
        String oldestDay = null;
        for (var entry : this.items.entrySet()) {
            String last = entry.getValue().isEmpty() ? "" : entry.getValue().getLast().day;
            if (oldestDay == null || last.compareTo(oldestDay) < 0) { oldestDay = last; oldestItem = entry.getKey(); }
        }
        if (oldestItem != null) this.items.remove(oldestItem);
    }

    /** Days between this record and today; a record that can't be read counts as too old to keep. */
    private static long age(Day d, LocalDate today) {
        try {
            return ChronoUnit.DAYS.between(LocalDate.parse(d.day), today);
        } catch (RuntimeException e) {
            return Long.MAX_VALUE;
        }
    }
}
