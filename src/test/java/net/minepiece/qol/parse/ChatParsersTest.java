package net.minepiece.qol.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatParsersTest {
    @Test
    void parsesSpanishBossLeaderboardLine() {
        assertEquals("Ace", ChatParsers.parseBossKill("-= Clasificación Ace =-").orElse(""));
    }
}
