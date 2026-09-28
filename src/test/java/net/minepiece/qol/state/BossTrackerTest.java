package net.minepiece.qol.state;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BossTrackerTest {
    @TempDir Path directory;

    private BossTracker tracker(PersistentState state) {
        return new BossTracker(state, ignored -> { }, new DebugLogManager(directory, false));
    }

    private void reward(BossTracker tracker, int bounty) {
        tracker.captureMinibossActionbar("+" + bounty + " 军 +1000 XP", 1, 2, 3);
    }

    @Test void tenRegularMobRewardsDoNotRegisterMiniboss() {
        PersistentState state = new PersistentState();
        BossTracker tracker = tracker(state);
        for (int bounty = 1; bounty <= 10; bounty++) reward(tracker, bounty);
        assertTrue(state.bosses.isEmpty());
    }

    @Test void progressiveCounterDoesNotRegisterMiniboss() {
        PersistentState state = new PersistentState();
        BossTracker tracker = tracker(state);
        for (int bounty : new int[] {1, 2, 5, 11}) reward(tracker, bounty);
        assertTrue(state.bosses.isEmpty());
    }

    @Test void instantTenRewardRegistersMiniboss() {
        PersistentState state = new PersistentState();
        reward(tracker(state), 10);
        assertEquals(1, state.bosses.size());
        assertTrue(state.bosses.values().iterator().next().miniboss);
    }

    @Test void jumpOfTenRegistersEvenWhenTotalExceedsBossThreshold() {
        PersistentState state = new PersistentState();
        BossTracker tracker = tracker(state);
        reward(tracker, 50);
        assertTrue(state.bosses.isEmpty());
        reward(tracker, 60);
        assertEquals(1, state.bosses.size());
    }

    @Test void jumpOfTenFromNonzeroCounterRegisters() {
        PersistentState state = new PersistentState();
        BossTracker tracker = tracker(state);
        reward(tracker, 5);
        reward(tracker, 15);
        assertEquals(1, state.bosses.size());
    }
}
