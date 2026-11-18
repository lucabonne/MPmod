package net.minepiece.qol.state;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
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
        assertEquals("» bosses.spawn 3 (bosses.current)", atIsland.getFirst().text());
        assertEquals("safes.purple: ?", atIsland.get(1).text());

        tracker.observe("3", 0, LOCKED, 1_000);
        tracker.observe("3", 1, "Status • Awaiting lockpicking •", 1_000);
        // Ready safes come first, then running timers.
        assertEquals("safes.green: safes.ready", tracker.lines(TR, 1_000, "3", false).get(1).text());
        assertEquals("safes.purple: 1m 50s", tracker.lines(TR, 1_000, "3", false).get(2).text());
        assertEquals("safes.purple: 1m 40s", tracker.lines(TR, 11_000, "3", false).get(2).text());
        assertEquals(SafeChestTracker.Status.READY, tracker.status("3", 0, 111_000));
        assertEquals(0xFFB46CFF, tracker.lines(TR, 0, "3", false).get(2).color());

        tracker.onChatMessage("3", "莱 Lock picking successful!", -1059.5, 212, 5741.5);
        assertEquals(SafeChestTracker.Status.PICKED, tracker.status("3", 1, 12_000));
        tracker.onChatMessage("3", "莱 Lock picking successful!", 0, 70, 0); // away from every safe: ignored
        tracker.observe("3", 1, "Lock Time: 2h 0m 0s", 13_000);
        assertEquals("safes.green: 2h 0m", tracker.lines(TR, 13_000, "3", false).get(2).text());
    }

    @Test void keepsEachSpawnSeparateAndSortsBySoonestTimer() {
        var tracker = new SafeChestTracker();
        tracker.observe("", 2, "Lock Time: 10s", 0);
        tracker.observe("12", 0, "Lock Time: 30s", 0);
        tracker.observe("2", 0, "Lock Time: 1m 0s", 0);
        var lines = tracker.lines(TR, 0, "2", true);
        assertEquals(21, lines.size()); // 3 spawns × (header + 6 safes)
        assertEquals("bosses.spawn common.unknown", lines.get(0).text());
        assertEquals("safes.orange: 10s", lines.get(1).text());
        assertEquals("bosses.spawn 12", lines.get(7).text());
        assertEquals("safes.purple: 30s", lines.get(8).text());
        assertEquals("» bosses.spawn 2 (bosses.current)", lines.get(14).text());
        assertEquals("safes.purple: 1m 0s", lines.get(15).text());
        assertEquals(SafeChestTracker.Status.UNKNOWN, tracker.status("2", 2, 0));
    }

    @Test void spawnsWithoutRunningTimersGoByNameWithUnknownLast() {
        var tracker = new SafeChestTracker();
        tracker.observe("", 0, "Status • Awaiting lockpicking •", 0);
        tracker.observe("12", 0, "Status • Awaiting lockpicking •", 0);
        tracker.observe("2", 0, "Status • Awaiting lockpicking •", 0);
        tracker.observe("5", 0, "Lock Time: 5h 0m 0s", 0);
        assertEquals(List.of("bosses.spawn 5", "bosses.spawn 12", "bosses.spawn 2", "bosses.spawn common.unknown"),
            headers(tracker.lines(TR, 0, "", false)));
    }

    @Test void closestTimerSpawnMovesToTheTop() {
        var tracker = new SafeChestTracker();
        // Spawn 3: red 1m, blue 3m, green 6m. Spawn 7: yellow 2m, purple 4m, orange 7m.
        tracker.observe("3", 3, "Lock Time: 1m 0s", 0);
        tracker.observe("3", 5, "Lock Time: 3m 0s", 0);
        tracker.observe("3", 1, "Lock Time: 6m 0s", 0);
        tracker.observe("7", 4, "Lock Time: 2m 0s", 0);
        tracker.observe("7", 0, "Lock Time: 4m 0s", 0);
        tracker.observe("7", 2, "Lock Time: 7m 0s", 0);

        var start = tracker.lines(TR, 0, "3", true);
        assertEquals(List.of("» bosses.spawn 3 (bosses.current)", "bosses.spawn 7"), headers(start));
        assertEquals(List.of("safes.red: 1m 0s", "safes.blue: 3m 0s", "safes.green: 6m 0s",
            "safes.purple: ?", "safes.orange: ?", "safes.yellow: ?"), texts(start.subList(1, 7)));

        // 1m 30s: spawn 3's red is ready, its next is 1m 30s; spawn 7's next is 30s.
        var later = tracker.lines(TR, 90_000, "3", true);
        assertEquals(List.of("bosses.spawn 7", "» bosses.spawn 3 (bosses.current)"), headers(later));
        assertEquals("safes.yellow: 30s", later.get(1).text());
        assertEquals("safes.red: safes.ready", later.get(8).text());
        assertEquals("safes.blue: 1m 30s", later.get(9).text());

        // 2m 30s: spawn 3's next is 30s again; spawn 7's next is 1m 30s.
        var after = tracker.lines(TR, 150_000, "3", true);
        assertEquals(List.of("» bosses.spawn 3 (bosses.current)", "bosses.spawn 7"), headers(after));
        assertEquals("safes.blue: 30s", after.get(2).text());
    }

    @Test void picksAfterTimersAndMarksOnlyTheCurrentSpawn() {
        var tracker = new SafeChestTracker();
        tracker.observe("4", 0, "Lock Time: 1m 0s", 0);
        tracker.observe("4", 2, "Lock Time: 10s", 0);
        tracker.onChatMessage("4", "Lock picking successful!", -1059.5, 212, 5741.5); // green
        var lines = tracker.lines(TR, 0, "9", false);
        assertEquals("bosses.spawn 4", lines.get(0).text()); // not the current spawn, not at the island: no marker, no extra spawn
        assertEquals(7, lines.size());
        assertEquals(List.of("safes.orange: 10s", "safes.purple: 1m 0s", "safes.green: safes.picked"),
            texts(lines.subList(1, 4)));
        assertEquals(0xFF97B9EA, lines.get(0).color());
        assertEquals(0xFFFFD36B, tracker.lines(TR, 0, "4", false).get(0).color());
    }

    private static List<String> headers(List<SafeChestTracker.Line> lines) {
        return texts(lines).stream().filter(text -> text.contains("bosses.spawn")).toList();
    }

    private static List<String> texts(List<SafeChestTracker.Line> lines) {
        return lines.stream().map(SafeChestTracker.Line::text).toList();
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

    @Test void timersSurviveSaveAndReloadUntilReset() {
        var state = new PersistentState();
        int[] saves = {0};
        var tracker = new SafeChestTracker();
        tracker.attachPersistence(state.safeChests, () -> saves[0]++);
        tracker.observe("3", 0, LOCKED, 1_000);
        tracker.observe("3", 0, "Lock Time: 1m 49s", 2_000); // same countdown a second later: no extra save
        tracker.observe("3", 1, "Status • Awaiting lockpicking •", 1_000);
        assertEquals(2, saves[0]);

        var gson = new com.google.gson.Gson();
        var reloaded = gson.fromJson(gson.toJson(state), PersistentState.class);
        var afterRestart = new SafeChestTracker();
        afterRestart.attachPersistence(reloaded.safeChests, () -> { });
        assertEquals("safes.purple: 1m 40s", afterRestart.lines(TR, 11_000, "3", false).get(2).text()); // away from the island
        assertEquals(SafeChestTracker.Status.READY, afterRestart.status("3", 1, 11_000));

        afterRestart.clear();
        assertTrue(reloaded.safeChests.isEmpty());
        assertFalse(afterRestart.hasData());
    }

    @Test void repairsBrokenSavedEntries() {
        var saved = new com.google.gson.Gson().fromJson("{\"safeChests\":{\"3\":{\"status\":[\"LOCKED\"],\"unlockAtMs\":[5000]},\"4\":{}}}", PersistentState.class);
        var tracker = new SafeChestTracker();
        tracker.attachPersistence(saved.safeChests, () -> { });
        assertEquals(SafeChestTracker.Status.LOCKED, tracker.status("3", 0, 0));
        assertEquals(SafeChestTracker.Status.UNKNOWN, tracker.status("3", 5, 0));
        assertEquals(SafeChestTracker.Status.UNKNOWN, tracker.status("4", 0, 0));
    }
}
