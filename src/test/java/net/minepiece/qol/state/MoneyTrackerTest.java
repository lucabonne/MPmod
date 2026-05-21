package net.minepiece.qol.state;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTrackerTest {
    @Test
    void queuesPositiveNonAhDeltaUntilFirstBalance() {
        PersistentState state = new PersistentState();
        MoneyTracker tracker = new MoneyTracker(state, ignored -> { }, Path.of("."));

        tracker.onChatMessage("You have withdrawn 100 实 from your island bank");
        assertFalse(tracker.isBalanceInitialized());
        assertEquals(0L, tracker.getCurrentBalance());
        assertEquals(0L, tracker.getNonAhToday());

        tracker.onCommandSent("balance");
        tracker.onChatMessage("Balance: 5,000 实");

        assertTrue(tracker.isBalanceInitialized());
        assertEquals(5_100L, tracker.getCurrentBalance());
        assertEquals(100L, tracker.getNonAhToday());
    }

    @Test
    void queuesNegativeNonAhDeltaUntilFirstBalance() {
        PersistentState state = new PersistentState();
        MoneyTracker tracker = new MoneyTracker(state, ignored -> { }, Path.of("."));

        tracker.onChatMessage("You paid 50 实");
        assertFalse(tracker.isBalanceInitialized());

        tracker.onCommandSent("balance");
        tracker.onChatMessage("Balance: 5,000 实");

        assertTrue(tracker.isBalanceInitialized());
        assertEquals(4_950L, tracker.getCurrentBalance());
        assertEquals(-50L, tracker.getNonAhToday());
    }

    @Test
    void parsesLocalizedPositiveMoneyDelta() {
        PersistentState state = new PersistentState();
        MoneyTracker tracker = new MoneyTracker(state, ignored -> { }, Path.of("."));
        tracker.setCurrentBalance(1_000L);

        tracker.onChatMessage("Hai guadagnato 1.250 实");

        assertEquals(2_250L, tracker.getCurrentBalance());
        assertEquals(1_250L, tracker.getNonAhToday());
    }

    @Test
    void parsesLocalizedNegativeMoneyDelta() {
        PersistentState state = new PersistentState();
        MoneyTracker tracker = new MoneyTracker(state, ignored -> { }, Path.of("."));
        tracker.setCurrentBalance(5_000L);

        tracker.onChatMessage("Has pagado 1.250 实");

        assertEquals(3_750L, tracker.getCurrentBalance());
        assertEquals(-1_250L, tracker.getNonAhToday());
    }
}
