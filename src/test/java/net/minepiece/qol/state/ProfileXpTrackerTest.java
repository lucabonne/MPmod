package net.minepiece.qol.state;

import net.minepiece.qol.parse.ProfileXpParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileXpTrackerTest {
    @Test
    void sharedItemXpUpdatesWithoutBarAndOldPercentDoesNotUndoIt() {
        ProfileXpTracker tracker = new ProfileXpTracker();
        tracker.sync(new ProfileXpParser.Snapshot(0, 63, 47784, 307450));
        tracker.addSharedXp(200);
        assertEquals(47984, tracker.getCurrentXp());
        tracker.updatePercent(new ProfileXpParser.BarProgress(63, 15.54, true), false);
        assertEquals(47984, tracker.getCurrentXp());
        assertFalse(tracker.getHudLines(key -> key).stream().anyMatch(line -> line.startsWith("progress.gained")));
        ProfileXpTracker restored = new ProfileXpTracker();
        restored.restore(tracker.snapshot());
        assertEquals(47984, restored.getCurrentXp());
        assertTrue(restored.isCached());
        restored.updatePercent(new ProfileXpParser.BarProgress(63, 15.61, true), false);
        assertFalse(restored.isCached());
        assertFalse(restored.needsSync());
        restored.updatePercent(new ProfileXpParser.BarProgress(64, 1, true), false);
        assertTrue(restored.needsSync());
        assertTrue(restored.getHudLines(key -> key).contains("progress.level_sync_hint"));
    }

    @Test
    void sharedXpCrossingUnknownLevelRequestsSyncAndPersistsThatRequirement() {
        ProfileXpTracker tracker = new ProfileXpTracker();
        tracker.sync(new ProfileXpParser.Snapshot(0, 63, 307440, 307450));
        tracker.addSharedXp(20);
        assertTrue(tracker.needsSync());
        ProfileXpTracker restored = new ProfileXpTracker();
        restored.restore(tracker.snapshot());
        assertTrue(restored.needsSync());
        assertTrue(restored.getHudLines(key -> key).getFirst().contains("64"));
    }

    @Test
    void baselineIsNotEarnedXpAndRoundedBarDoesNotChangeExactSnapshot() {
        ProfileXpTracker tracker = new ProfileXpTracker();
        tracker.sync(new ProfileXpParser.Snapshot(0, 63, 47784, 307450));
        tracker.updatePercent(new ProfileXpParser.BarProgress(63, 15.54, true));
        assertEquals(47784, tracker.getCurrentXp());
        assertEquals(0, tracker.getGainedXp());
        tracker.updatePercent(new ProfileXpParser.BarProgress(63, 15.64, true));
        assertEquals(307.45, tracker.getGainedXp(), 0.001);
        tracker.updatePercent(new ProfileXpParser.BarProgress(63, 15.64, true));
        assertEquals(307.45, tracker.getGainedXp(), 0.001);
        tracker.sync(new ProfileXpParser.Snapshot(0, 63, 48090, 307450));
        assertEquals(306, tracker.getGainedXp());
        assertEquals(48090, tracker.getCurrentXp());
    }

    @Test
    void levelUpWaitsForNewRequirementThenReconcilesExactGains() {
        ProfileXpTracker tracker = new ProfileXpTracker();
        tracker.sync(new ProfileXpParser.Snapshot(0, 63, 307000, 307450));
        tracker.updatePercent(new ProfileXpParser.BarProgress(64, 1, true));
        assertTrue(tracker.needsSync());
        assertTrue(Double.isNaN(tracker.getCurrentXp()));
        tracker.sync(new ProfileXpParser.Snapshot(0, 64, 3200, 320000));
        assertEquals(3650, tracker.getGainedXp());
        assertFalse(tracker.needsSync());
        tracker.updatePercent(new ProfileXpParser.BarProgress(64, 2, true));
        assertEquals(6850, tracker.getGainedXp());
    }

    @Test
    void percentageWrapWithoutLevelRequiresSync() {
        ProfileXpTracker tracker = new ProfileXpTracker();
        tracker.sync(new ProfileXpParser.Snapshot(0, 10, 900, 1000));
        tracker.updatePercent(new ProfileXpParser.BarProgress(null, 5, false));
        assertTrue(tracker.needsSync());
        tracker.sync(new ProfileXpParser.Snapshot(0, 11, 100, 2000));
        assertEquals(200, tracker.getGainedXp());
    }

    @Test
    void skipsUnknownLevelRequirementsAndResetsOnAscensionOrReconnect() {
        ProfileXpTracker tracker = new ProfileXpTracker();
        assertTrue(Double.isNaN(tracker.getGainedXp()));
        tracker.sync(new ProfileXpParser.Snapshot(0, 10, 100, 1000));
        tracker.sync(new ProfileXpParser.Snapshot(0, 13, 200, 4000));
        assertTrue(Double.isNaN(tracker.getGainedXp()));
        tracker.sync(new ProfileXpParser.Snapshot(1, 1, 0, 100));
        assertEquals(0, tracker.getGainedXp());
        tracker.reset();
        assertNull(tracker.getAnchor());
    }

    @Test
    void bindsUnlabelledBarOnlyWhenItMatchesTheKnownProfile() {
        ProfileXpParser.Snapshot profile = new ProfileXpParser.Snapshot(0, 63, 47784, 307450);
        assertTrue(ProgressHudController.matchesProfileBar(new ProfileXpParser.BarProgress(null, 15.54, false), profile));
        assertFalse(ProgressHudController.matchesProfileBar(new ProfileXpParser.BarProgress(null, 50, false), profile));
        assertFalse(ProgressHudController.matchesProfileBar(new ProfileXpParser.BarProgress(20, 15.54, false), profile));
        assertTrue(ProgressHudController.matchesProfileBar(new ProfileXpParser.BarProgress(63, 16, true), profile));
    }
}
