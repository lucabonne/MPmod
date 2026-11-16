package net.minepiece.qol.state;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SafeChestTrackerTest {
    private static final Function<String, String> TR = key -> key;
    private static final String LOCKED = "Safe Information • Lock Time: 1m 50s Status • Locked •";

    @Test void parsesLockTimeFormats() {
        assertEquals(110, SafeChestTracker.parseLockSeconds(LOCKED).getAsLong());
        assertEquals(59, SafeChestTracker.parseLockSeconds("• Lock Time: 59s").getAsLong());
        assertEquals(3723, SafeChestTracker.parseLockSeconds("Lock Time: 1h 2m 3s").getAsLong());
        assertTrue(SafeChestTracker.parseLockSeconds("Status • Awaiting lockpicking •").isEmpty());
    }

    @Test void matchesHologramsToTheirSafeByPosition() {
        assertEquals(0, SafeChestTracker.safeAt(-1085.5, 231, 5698.5)); // purple, text above the safe
        assertEquals(5, SafeChestTracker.safeAt(-939.5, 214, 5743.5));  // blue
        assertEquals(-1, SafeChestTracker.safeAt(-1000, 230, 5700));    // between safes
        assertTrue(SafeChestTracker.nearIsland(-995, 223, 5670));
        assertFalse(SafeChestTracker.nearIsland(0, 70, 0));
    }

    @Test void countsDownThenReadyAndMarksPickedSafe() {
        var tracker = new SafeChestTracker();
        assertFalse(tracker.hasData());
        assertTrue(tracker.lines(TR, 0, "3", false).isEmpty());
        var atIsland = tracker.lines(TR, 0, "3", true);
        assertEquals("bosses.spawn 3", atIsland.getFirst().text());
        assertEquals("safes.purple: ?", atIsland.get(1).text());

        tracker.observe("3", 0, LOCKED, 1_000);
        tracker.observe("3", 1, "Status • Awaiting lockpicking •", 1_000);
        assertEquals("safes.purple: 1m 50s", tracker.lines(TR, 1_000, "3", false).get(1).text());
        assertEquals("safes.purple: 1m 40s", tracker.lines(TR, 11_000, "3", false).get(1).text());
        assertEquals("safes.green: safes.ready", tracker.lines(TR, 11_000, "3", false).get(2).text());
        assertEquals(SafeChestTracker.Status.READY, tracker.status("3", 0, 111_000));
        assertEquals(0xFFB46CFF, tracker.lines(TR, 0, "3", false).get(1).color());

        tracker.onChatMessage("3", "莱 Lock picking successful!", -1059.5, 212, 5741.5);
        assertEquals(SafeChestTracker.Status.PICKED, tracker.status("3", 1, 12_000));
        tracker.onChatMessage("3", "莱 Lock picking successful!", 0, 70, 0); // away from every safe: ignored
        tracker.observe("3", 1, "Lock Time: 2h 0m 0s", 13_000);
        assertEquals("safes.green: 2h 0m", tracker.lines(TR, 13_000, "3", false).get(2).text());
    }

    @Test void keepsEachSpawnSeparateAndUnknownLast() {
        var tracker = new SafeChestTracker();
        tracker.observe("", 2, "Lock Time: 10s", 0);
        tracker.observe("12", 0, "Lock Time: 30s", 0);
        tracker.observe("2", 0, "Lock Time: 1m 0s", 0);
        var lines = tracker.lines(TR, 0, "2", true);
        assertEquals(21, lines.size()); // 3 spawns × (header + 6 safes)
        assertEquals("bosses.spawn 12", lines.get(0).text());
        assertEquals("safes.purple: 30s", lines.get(1).text());
        assertEquals("bosses.spawn 2", lines.get(7).text());
        assertEquals("safes.purple: 1m 0s", lines.get(8).text());
        assertEquals("bosses.spawn common.unknown", lines.get(14).text());
        assertEquals("safes.orange: 10s", lines.get(17).text());
        assertEquals(SafeChestTracker.Status.UNKNOWN, tracker.status("2", 2, 0));
    }

    @Test void resetForgetsSpawnsAndTimersUntilBackAtIsland() {
        var tracker = new SafeChestTracker();
        tracker.observe("3", 0, LOCKED, 0);
        tracker.clear();
        assertFalse(tracker.hasData());
        assertTrue(tracker.lines(TR, 0, "3", false).isEmpty());
        assertEquals(SafeChestTracker.Status.UNKNOWN, tracker.status("3", 0, 0));
        assertEquals("safes.purple: ?", tracker.lines(TR, 0, "3", true).get(1).text()); // back at the island
    }
}
