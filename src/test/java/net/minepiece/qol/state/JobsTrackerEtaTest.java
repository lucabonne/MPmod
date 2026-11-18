package net.minepiece.qol.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobsTrackerEtaTest {
    @TempDir Path dir;

    private JobsTracker tracker(double current, double needed) {
        PersistentState state = new PersistentState();
        PersistentState.JobInfo info = new PersistentState.JobInfo();
        info.currentXp = current;
        info.neededXp = needed;
        state.jobs.put("fisherman", info);
        state.currentJob = "fisherman";
        return new JobsTracker(state, ignored -> { }, new DebugLogManager(dir, false));
    }

    @Test
    void estimatesFromTheRecentXpRate() {
        JobsTracker jobs = tracker(400, 1_000);
        for (int i = 0; i <= 10; i++) jobs.recordXpRate("fisherman", 10, i * 12_000L); // 110 XP over two minutes
        // 600 XP left at 110 XP / 120 s
        assertEquals((long) Math.ceil(600 / (110 / 120.0)), jobs.secondsToNextLevel(120_000));
    }

    @Test
    void unknownWhenIdleOrNoProgressRead() {
        JobsTracker jobs = tracker(400, 1_000);
        assertEquals(-1, jobs.secondsToNextLevel(0));
        jobs.recordXpRate("fisherman", 10, 0);
        assertEquals(-1, jobs.secondsToNextLevel(3 * 60_000L));   // nothing for three minutes
        assertEquals(-1, tracker(-1, 1_000).secondsToNextLevel(0));
    }

    @Test
    void switchingJobsStartsAFreshRate() {
        JobsTracker jobs = tracker(400, 1_000);
        jobs.recordXpRate("farmer", 1_000, 0);
        jobs.recordXpRate("fisherman", 6, 30_000);
        // only the fisherman's 6 XP count, over the one-minute floor: 600 / (6 / 60 s)
        assertEquals(6_000, jobs.secondsToNextLevel(30_000));
    }
}
