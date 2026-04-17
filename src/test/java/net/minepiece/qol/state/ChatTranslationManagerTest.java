package net.minepiece.qol.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTranslationManagerTest {
    @Test
    void classifyPublicLineWithDivider() {
        ChatTranslationManager.ChatLineClassification classified = ChatTranslationManager.classifyLine("Luca › Hello there");

        assertTrue(classified.valid());
        assertEquals(ChatTranslationManager.ChatLineType.PUBLIC, classified.lineType());
        assertEquals("Luca", classified.authorPrefix());
        assertEquals("Hello there", classified.messageBody());
    }

    @Test
    void classifyPrivateLineFromPrefix() {
        ChatTranslationManager.ChatLineClassification classified = ChatTranslationManager.classifyLine("from Luca › hey");

        assertTrue(classified.valid());
        assertEquals(ChatTranslationManager.ChatLineType.PRIVATE, classified.lineType());
        assertEquals("from Luca", classified.authorPrefix());
        assertEquals("hey", classified.messageBody());
    }

    @Test
    void classifySystemLineWithoutDivider() {
        ChatTranslationManager.ChatLineClassification classified = ChatTranslationManager.classifyLine("Server restart in 5 minutes");

        assertTrue(classified.valid());
        assertEquals(ChatTranslationManager.ChatLineType.SYSTEM, classified.lineType());
        assertEquals("", classified.authorPrefix());
        assertEquals("Server restart in 5 minutes", classified.messageBody());
    }

    @Test
    void classifyBlankLineAsInvalid() {
        ChatTranslationManager.ChatLineClassification classified = ChatTranslationManager.classifyLine("   ");

        assertFalse(classified.valid());
        assertEquals(ChatTranslationManager.ChatLineType.INVALID, classified.lineType());
    }

    @Test
    void routeEnablingMatchesLineType() {
        assertTrue(ChatTranslationManager.isLineTypeEnabled(
            ChatTranslationManager.ChatLineType.PUBLIC,
            true,
            false,
            false
        ));
        assertTrue(ChatTranslationManager.isLineTypeEnabled(
            ChatTranslationManager.ChatLineType.PRIVATE,
            false,
            true,
            false
        ));
        assertTrue(ChatTranslationManager.isLineTypeEnabled(
            ChatTranslationManager.ChatLineType.SYSTEM,
            false,
            false,
            true
        ));
        assertFalse(ChatTranslationManager.isLineTypeEnabled(
            ChatTranslationManager.ChatLineType.INVALID,
            true,
            true,
            true
        ));
    }
}
