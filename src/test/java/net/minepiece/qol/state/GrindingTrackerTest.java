package net.minepiece.qol.state;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GrindingTrackerTest {
    @Test void boosterCorrectionAdjustsTotalsAndSmoothedRateWithoutRestartingTimer() {
        GrindingTracker tracker = new GrindingTracker(ZoneId.of("UTC"));
        tracker.onActionbar("+1 军 +100 实", 1000);
        tracker.onXpGain(100, 1000);
        double rate = tracker.getXpRate();
        double moneyRate = tracker.getMoneyRate();
        tracker.correctXp(100, 4000);
        assertEquals(200, tracker.getXp());
        assertEquals(rate * 1.2, tracker.getXpRate(), 0.001);
        assertEquals(moneyRate, tracker.getMoneyRate());
        assertFalse(tracker.isActive(61000));
        tracker.reset();
        tracker.correctXp(100, 62000);
        assertEquals(0, tracker.getXp());
        assertEquals(-1, tracker.getXpRate());
    }

    @Test void rateChangesOnlyOnRewardsAndIsSmoothed() {
        GrindingTracker tracker = new GrindingTracker(ZoneId.of("UTC"));
        tracker.onActionbar("+1 军 +100 实", 1000);
        tracker.onXpGain(100, 1000);
        double first = tracker.getXpRate();
        for (int now = 1100; now <= 1900; now += 100) {
            tracker.tick(now);
            assertEquals(first, tracker.getXpRate());
        }
        tracker.onActionbar("+2 军 +200 实", 2000);
        tracker.onXpGain(100, 2000);
        assertEquals(first, tracker.getXpRate()); // At most one display update per five seconds.
        tracker.onActionbar("+1 军 +100 实", 7000);
        tracker.onXpGain(100, 7000);
        assertTrue(tracker.getXpRate() > first);
        assertTrue(tracker.getXpRate() < 300 * 3_600_000.0 / 10_000);
        double displayed = tracker.getXpRate();
        tracker.tick(50000);
        assertEquals(displayed, tracker.getXpRate());
        assertTrue(tracker.getHudLines(key -> key, 50000).stream().anyMatch(line -> line.equals("progress.gained: ~300")));
    }

    @Test void onlyDangerRewardsCountMoneyAndCombatXp() {
        GrindingTracker tracker = new GrindingTracker();
        tracker.onActionbar("+500 实", 1000);
        tracker.onXpGain(50, 1000);
        assertFalse(tracker.isActive(1000));
        assertEquals(0, tracker.getMoney());
        assertEquals(0, tracker.getXp());
        tracker.onActionbar("+1 军 +600 实", 2000);
        tracker.onXpGain(50, 2000);
        assertTrue(tracker.isActive(2000));
        assertEquals(100, tracker.getMoney());
        assertEquals(50, tracker.getXp());
        tracker.onActionbar("+1000 实", 2500);
        assertEquals(100, tracker.getMoney());
    }

    @Test void pausesOneMinuteAfterLastXpNotAfterLastDangerPacket() {
        GrindingTracker tracker = new GrindingTracker();
        tracker.onActionbar("+1 军 +100 实", 1000);
        tracker.onXpGain(50, 1000);
        tracker.tick(60999);
        assertTrue(tracker.isActive(60999));
        tracker.onActionbar("+2 军 +200 实", 61000);
        assertFalse(tracker.isActive(61000));
        assertEquals(60000, tracker.getActiveMs());
        tracker.tick(120000);
        assertEquals(60000, tracker.getActiveMs());
        tracker.onActionbar("+1 军 +100 实", 121000);
        tracker.onXpGain(100, 121000);
        tracker.tick(122000);
        assertTrue(tracker.isActive(122000));
        assertEquals(61000, tracker.getActiveMs());
    }

    @Test void repeatedActionbarAndUnchangedXpDoNotDoubleCount() {
        GrindingTracker tracker = new GrindingTracker();
        tracker.observeProfileXp(0, 0);
        tracker.onActionbar("+1 军 +100 实", 1000);
        tracker.observeProfileXp(50, 1000);
        for (int now = 2000; now <= 62000; now += 1000) {
            tracker.onActionbar("+1 军 +100 实", now);
            tracker.observeProfileXp(50, now);
        }
        assertEquals(100, tracker.getMoney());
        assertEquals(50, tracker.getXp());
        assertEquals(60000, tracker.getActiveMs());
        assertFalse(tracker.isActive(62000));
    }

    @Test void resetsAtLocalMidnightAndKeepsTotalsThroughSameDayReconnect() {
        ZoneId zone = ZoneId.of("Europe/Rome");
        long before = ZonedDateTime.of(2026, 9, 27, 23, 59, 50, 0, zone).toInstant().toEpochMilli();
        GrindingTracker tracker = new GrindingTracker(zone);
        tracker.onActionbar("+1 军 +100 实", before);
        tracker.onXpGain(50, before);
        var saved = tracker.snapshot(before + 1000);
        GrindingTracker restored = new GrindingTracker(zone);
        restored.restore(saved, before + 5000);
        assertEquals(100, restored.getMoney());
        assertEquals(1000, restored.getActiveMs());
        assertFalse(restored.isActive(before + 5000));
        restored.tick(before + 10_000);
        assertEquals(0, restored.getMoney());
        assertEquals(0, restored.getXp());
        assertEquals(0, restored.getActiveMs());
        assertEquals(-1, restored.getXpRate());
        restored.restore(saved, before + 20_000);
        assertEquals(0, restored.getMoney());
    }

    @Test void midnightDoesNotRecountTheRewardAlreadyOnScreen() {
        ZoneId zone = ZoneId.of("UTC");
        long before = ZonedDateTime.of(2026, 9, 27, 23, 59, 59, 0, zone).toInstant().toEpochMilli();
        GrindingTracker tracker = new GrindingTracker(zone);
        tracker.onActionbar("+1 军 +100 实", before);
        tracker.onXpGain(10, before);
        tracker.onActionbar("+1 军 +100 实", before + 1000);
        assertEquals(0, tracker.getMoney());
        assertFalse(tracker.isActive(before + 1000));
    }
}
