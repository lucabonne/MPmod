package net.minepiece.qol.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownTrackerTest {
    @Test
    void recognizesSpanishHakiActivationMessage() {
        CooldownTracker tracker = new CooldownTracker();

        tracker.onChatMessage("!Has activado el haki.");

        assertTrue(tracker.getHakiRemainingMillis() > 0L);
        assertTrue(CooldownTracker.isHakiUsedMessage("!Has activado el haki."));
    }

    @Test
    void recognizesSpanishHakiReadyMessage() {
        CooldownTracker tracker = new CooldownTracker();
        tracker.onChatMessage("!Has activado el haki.");

        tracker.onChatMessage("!Puedes usar tu haki.");

        assertEquals(0L, tracker.getHakiRemainingMillis());
        assertEquals("Haki ready", tracker.getHakiHudText());
        assertTrue(CooldownTracker.isHakiReadyMessage("!Puedes usar tu haki."));
    }
}
