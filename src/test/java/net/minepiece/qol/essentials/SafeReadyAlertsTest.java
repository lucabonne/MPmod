package net.minepiece.qol.essentials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minepiece.qol.state.SafeChestTracker;
import org.junit.jupiter.api.Test;

class SafeReadyAlertsTest {
    @Test
    void alertsOnceWhenATimerRunsOut() {
        SafeChestTracker safes = new SafeChestTracker();
        safes.observe("3", 1, "Lock time: 1m", 0);
        SafeReadyAlerts alerts = new SafeReadyAlerts();
        assertTrue(alerts.check(safes, k -> k.equals("safes.alert") ? "%s ready on %s" : k, 1_000).isEmpty());
        var fired = alerts.check(safes, k -> k.equals("safes.alert") ? "%s ready on %s" : k, 61_000);
        assertEquals(1, fired.size());
        assertEquals("safes.green ready on 3", fired.getFirst().text());
        assertTrue(alerts.check(safes, k -> k, 62_000).isEmpty());
    }

    @Test
    void safesAlreadyReadyWhenFirstSeenStayQuiet() {
        SafeChestTracker safes = new SafeChestTracker();
        safes.observe("3", 0, "Awaiting lockpicking", 0);
        SafeReadyAlerts alerts = new SafeReadyAlerts();
        assertTrue(alerts.check(safes, k -> k, 0).isEmpty());
        assertTrue(alerts.check(safes, k -> k, 5_000).isEmpty());
    }
}
