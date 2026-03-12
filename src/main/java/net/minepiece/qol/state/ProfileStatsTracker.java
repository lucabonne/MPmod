package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minepiece.qol.parse.TooltipParsers;

public final class ProfileStatsTracker {
    private Double power;
    private Double strength;
    private Double speed;
    private long lastUpdateMs;
    private boolean awaitingProfileHoverSync;
    private int lastSelectedSlot = -1;
    private final Map<Integer, TooltipParsers.ProfileStatsData> learnedSlotStats = new HashMap<>();

    public void update(TooltipParsers.ProfileStatsData statsData, int selectedSlot, boolean rememberSlot) {
        applySnapshot(statsData);
        this.lastUpdateMs = System.currentTimeMillis();
        this.lastSelectedSlot = selectedSlot;
        if (rememberSlot && selectedSlot >= 0) {
            this.learnedSlotStats.put(selectedSlot, statsData);
        }
    }

    public void armProfileHoverSync() {
        this.awaitingProfileHoverSync = true;
    }

    public boolean consumeProfileHoverSync(TooltipParsers.ProfileStatsData statsData) {
        if (!this.awaitingProfileHoverSync || statsData == null) {
            return false;
        }
        if (statsData.power() == null || statsData.strength() == null || statsData.speed() == null) {
            return false;
        }
        applySnapshot(statsData);
        this.lastUpdateMs = System.currentTimeMillis();
        this.awaitingProfileHoverSync = false;
        return true;
    }

    public boolean isAwaitingProfileHoverSync() {
        return this.awaitingProfileHoverSync;
    }

    public void onSelectedSlotChanged(int selectedSlot) {
        if (selectedSlot == this.lastSelectedSlot) {
            return;
        }

        TooltipParsers.ProfileStatsData nextStats = this.learnedSlotStats.get(selectedSlot);
        if (nextStats != null) {
            applySnapshot(nextStats);
        }

        this.lastSelectedSlot = selectedSlot;
    }

    public List<String> getHudLines() {
        List<String> lines = new ArrayList<>(3);
        lines.add("Power: " + formatValue(this.power));
        lines.add("Strength: " + formatValue(this.strength));
        lines.add("Speed: " + formatValue(this.speed));
        return lines;
    }

    public long getLastUpdateMs() {
        return this.lastUpdateMs;
    }

    private void applySnapshot(TooltipParsers.ProfileStatsData statsData) {
        if (statsData.power() != null) {
            this.power = statsData.power();
        }
        if (statsData.strength() != null) {
            this.strength = statsData.strength();
        }
        if (statsData.speed() != null) {
            this.speed = statsData.speed();
        }
    }

    private static String formatValue(Double value) {
        if (value == null) {
            return "?";
        }
        if (Math.floor(value) == value) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
