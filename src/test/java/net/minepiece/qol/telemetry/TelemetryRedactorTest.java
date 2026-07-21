package net.minepiece.qol.telemetry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryRedactorTest {
    @Test
    void redactsAuthenticationArguments() {
        assertEquals("/login <redacted>", TelemetryRedactor.redactCommand("/login hunter2"));
        assertEquals("register <redacted>", TelemetryRedactor.redactCommand("register secret secret"));
        assertEquals("/is chest 4", TelemetryRedactor.redactCommand("/is chest 4"));
    }

    @Test
    void escapesSpreadsheetFormulas() {
        assertEquals("'=SUM(A1:A2)", TelemetryRedactor.excelSafe("=SUM(A1:A2)"));
        assertEquals("safe", TelemetryRedactor.excelSafe("safe"));
    }

    @Test
    void limitsSessionsToMinepieceHosts() {
        assertTrue(TelemetryManager.isMinepieceAddress("play.minepiece.net"));
        assertTrue(TelemetryManager.isMinepieceAddress("eu.play.minepiece.net:25565"));
        assertFalse(TelemetryManager.isMinepieceAddress("minepiece.net"));
        assertFalse(TelemetryManager.isMinepieceAddress("play.minepiece.net.evil.example"));
    }

    @Test
    void validatesOnlyOfficialDiscordWebhooks() {
        assertTrue(DiscordWebhookClient.isValidWebhook("https://discord.com/api/webhooks/123/token"));
        assertFalse(DiscordWebhookClient.isValidWebhook("http://discord.com/api/webhooks/123/token"));
        assertFalse(DiscordWebhookClient.isValidWebhook("https://discord.example/api/webhooks/123/token"));
    }
}
