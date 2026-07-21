package net.minepiece.qol.telemetry;

public record TelemetryEvent(
    String sessionId,
    long sequence,
    long timestampMs,
    Category category,
    String type,
    String dataJson
) {
    public enum Category {
        SESSION,
        INVENTORY,
        ECONOMY,
        COMBAT,
        CHAT_COMMAND,
        INTERACTION,
        SYSTEM
    }
}
