package net.minepiece.qol.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyMessageClassifierTest {
    @Test
    void rejectsFormattedPlayerMoneyChatEvenWhenDeliveredAsGameMessage() {
        assertFalse(MoneyMessageClassifier.isEligibleServerMessage(
            "武 | 要登在 Lukee › ill spent 2000000 on it", false
        ));
    }

    @Test
    void acceptsServerBankMessage() {
        assertTrue(MoneyMessageClassifier.isEligibleServerMessage(
            "莱 You have deposited 2M 实 in your island bank.", false
        ));
    }

    @Test
    void leavesActionbarToDedicatedParser() {
        assertFalse(MoneyMessageClassifier.isEligibleServerMessage("+50 实", true));
    }
}
