package net.minepiece.qol.essentials;

/** Coalesces kill notifications; server snapshots remain the source of mission progress. */
public final class PassRefreshSchedule {
    private long killDue = -1;
    private long lastRead = -5000;
    private long nextPeriodic;

    public void onBossKilled(long now) {
        killDue = killDue < 0 ? now + 2000 : Math.min(killDue, now + 2000);
    }

    public boolean due(long now, boolean incompleteMissions) {
        return now - lastRead >= 5000 && (killDue >= 0 && now >= killDue
            || incompleteMissions && now >= nextPeriodic);
    }

    public void readStarted(long now) {
        if (killDue >= 0) killDue = now + 30000;
        lastRead = now;
        nextPeriodic = now + 30000;
    }

    public void snapshotRead(long now) { nextPeriodic = now + 30000; killDue = -1; }

    public void reset() { killDue = -1; lastRead = -5000; nextPeriodic = 0; }
}
