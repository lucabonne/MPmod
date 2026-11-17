package net.minepiece.qol.state;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedItemXpTrackerTest {
    private SharedItemXpTracker.Progress xp(int level, long current, long required) {
        return new SharedItemXpTracker.Progress(level, current, required);
    }
    @Test void simultaneousAndDelayedFruitWeaponUpdatesAreCountedOnce() {
        var tracker = new SharedItemXpTracker();
        assertEquals(0, tracker.observe(Map.of("fruit", xp(1, 100, 1000), "weapon", xp(1, 200, 1000))));
        assertEquals(50, tracker.observe(Map.of("fruit", xp(1, 150, 1000), "weapon", xp(1, 250, 1000))));
        assertEquals(50, tracker.observe(Map.of("fruit", xp(1, 200, 1000), "weapon", xp(1, 250, 1000))));
        assertEquals(0, tracker.observe(Map.of("fruit", xp(1, 200, 1000), "weapon", xp(1, 300, 1000))));
    }
    @Test void noncombatUpgradeCanBeRebaselinedBeforeReturningToCombat() {
        var tracker = new SharedItemXpTracker();
        tracker.observe(Map.of("fruit", xp(1, 100, 10000), "weapon", xp(1, 100, 10000)));
        tracker.observe(Map.of("fruit", xp(1, 100, 10000), "weapon", xp(1, 9000, 10000)));
        tracker.alignSources();
        assertEquals(50, tracker.observe(Map.of("fruit", xp(1, 150, 10000))));
    }

    @Test void newItemsAndItemRemovalDoNotInventXp() {
        var tracker = new SharedItemXpTracker();
        tracker.observe(Map.of("fruit", xp(1, 100, 1000)));
        assertEquals(50, tracker.observe(Map.of("fruit", xp(1, 150, 1000))));
        assertEquals(0, tracker.observe(Map.of("weapon", xp(12, 8000, 10000))));
        assertEquals(10, tracker.observe(Map.of("weapon", xp(12, 8010, 10000))));
        tracker.reset();
        assertEquals(0, tracker.observe(Map.of("weapon", xp(12, 9000, 10000))));
    }
    @Test void levelRolloverCountsRemainderAndAscensionRebaselines() {
        var tracker = new SharedItemXpTracker();
        tracker.observe(Map.of("weapon", xp(1, 990, 1000)));
        assertEquals(30, tracker.observe(Map.of("weapon", xp(2, 20, 2000))));
        assertEquals(0, tracker.observe(Map.of("weapon", xp(1, 0, 1000))));
        assertEquals(0, tracker.observe(Map.of("weapon", xp(4, 100, 4000))));
    }
}
