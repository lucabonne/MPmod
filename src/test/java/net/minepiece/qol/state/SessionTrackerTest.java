package net.minepiece.qol.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SessionTrackerTest {
    @Test
    void addsWhatDailyCountersGrewWhileConnected() {
        SessionTracker session = new SessionTracker();
        session.start(0);
        session.poll(1_000, "d1", 500, 100, 50, 1_000, 200);   // baseline: earned before joining
        session.poll(2_000, "d1", 400, 300, 50, 1_500, 260);   // spent 100, sold 200
        session.onBossKill();
        session.onSafePicked();
        var s = session.end(3_000);
        assertEquals(100, s.money, 1e-9);    // -100 non-AH + 200 sold
        assertEquals(200, s.ahSold, 1e-9);
        assertEquals(0, s.ahBought, 1e-9);
        assertEquals(500, s.combatXp, 1e-9);
        assertEquals(60, s.jobXp, 1e-9);
        assertEquals(1, s.bosses);
        assertEquals(1, s.safes);
        assertNull(session.end(4_000));
    }

    @Test
    void midnightResetsCountFromZero() {
        SessionTracker session = new SessionTracker();
        session.start(0);
        session.poll(1_000, "d1", 1_000, 500, 0, 5_000, 900);
        session.poll(2_000, "d2", -50, 20, 0, 30, 10);          // new day: counters restarted
        var s = session.current(2_000);
        assertEquals(-50 + 20, s.money, 1e-9);
        assertEquals(20, s.ahSold, 1e-9);
        assertEquals(30, s.combatXp, 1e-9);
        assertEquals(10, s.jobXp, 1e-9);
    }

    @Test
    void summaryShowsTimeAndHourlyMoney() {
        var s = new SessionTracker.Summary();
        s.startMs = 0;
        s.endMs = 90 * 60_000L;
        s.money = 90_000;
        var lines = SessionTracker.lines(s, k -> k, "session.title");
        assertEquals("session.title · 1h 30m", lines.get(0));
        assertEquals("session.money: +90,000 (+60,000/h)", lines.get(1));
    }
}
