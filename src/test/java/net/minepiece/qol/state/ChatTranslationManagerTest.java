package net.minepiece.qol.state;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTranslationManagerTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void neverTranslatesEnglishIntoEnglish(boolean aggressive) {
        List<ChatTranslationManager.Rule> rules = List.of(new ChatTranslationManager.Rule("fr", "en"));
        assertNull(ChatTranslationManager.selectMatchingRule("hello there", rules,
            new ChatTranslationManager.TranslationResult("Hello there!", "en"), aggressive));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "fr", "es", "de", "it", "pt", "pl", "id", "tr"})
    void suppressesMessagesAlreadyInEachTargetLanguage(String target) {
        String source = target.equals("fr") ? "en" : "fr";
        assertNull(ChatTranslationManager.selectMatchingRule("original", List.of(new ChatTranslationManager.Rule(source, target)),
            new ChatTranslationManager.TranslationResult("rephrased", target), true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void matchesFrenchIncludingRegionalLanguageCodes(boolean aggressive) {
        ChatTranslationManager.Rule french = new ChatTranslationManager.Rule("fr", "en");
        assertEquals(french, ChatTranslationManager.selectMatchingRule("Bonjour, où est le boss ?", List.of(french),
            new ChatTranslationManager.TranslationResult("Hello, where is the boss?", "fr-FR"), aggressive));
    }

    @ParameterizedTest
    @ValueSource(strings = {"es", "de", "it", "pt", "pl", "id", "tr"})
    void matchesOtherConfiguredSources(String source) {
        ChatTranslationManager.Rule rule = new ChatTranslationManager.Rule(source, "en");
        assertEquals(rule, ChatTranslationManager.selectMatchingRule("original", List.of(
            new ChatTranslationManager.Rule("fr", "en"), rule),
            new ChatTranslationManager.TranslationResult("translated", source), false));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void suppressesUnchangedTranslationsRegardlessOfCaseAndSpacing(boolean aggressive) {
        assertNull(ChatTranslationManager.selectMatchingRule("  Hello   there  ",
            List.of(new ChatTranslationManager.Rule("fr", "en")),
            new ChatTranslationManager.TranslationResult("hello there", "fr"), aggressive));
    }

    @Test
    void aggressiveFallbackUsesActualDetectedSource() {
        List<ChatTranslationManager.Rule> rules = List.of(new ChatTranslationManager.Rule("fr", "en"));
        ChatTranslationManager.TranslationResult translated =
            new ChatTranslationManager.TranslationResult("Where is the boss?", "es");
        assertNull(ChatTranslationManager.selectMatchingRule("¿Dónde está el jefe?", rules, translated, false));
        assertEquals(new ChatTranslationManager.Rule("es", "en"),
            ChatTranslationManager.selectMatchingRule("¿Dónde está el jefe?", rules, translated, true));
    }

    @Test
    void missingDetectionAndEmptyResultsDoNotCreateFakeTranslations() {
        List<ChatTranslationManager.Rule> rules = List.of(new ChatTranslationManager.Rule("fr", "en"));
        assertNull(ChatTranslationManager.selectMatchingRule("bonjour", rules, null, true));
        assertNull(ChatTranslationManager.selectMatchingRule("bonjour", rules,
            new ChatTranslationManager.TranslationResult("hello", ""), true));
        assertNull(ChatTranslationManager.selectMatchingRule("bonjour", rules,
            new ChatTranslationManager.TranslationResult("", "fr"), true));
        assertNull(ChatTranslationManager.selectMatchingRule("bonjour", List.of(),
            new ChatTranslationManager.TranslationResult("hello", "fr"), true));
    }

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
