package net.minepiece.qol.telemetry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class TelemetryDiscordFormatter {
    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final int GREEN = 0x237A4B;
    private static final int AMBER = 0xC47A16;

    private TelemetryDiscordFormatter() {
    }

    static String sessionPayload(TelemetrySessionReport report, boolean attachmentReady) {
        JsonObject payload = new JsonObject();
        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        embed.addProperty("title", report.label + " — Session complete");
        embed.addProperty("description", report.session.cleanEnd()
            ? "Minepiece session recorded successfully."
            : "The session ended without a clean disconnect; available data is shown below.");
        embed.addProperty("color", report.session.cleanEnd() && report.session.droppedEvents() == 0L ? GREEN : AMBER);
        embed.addProperty("timestamp", (report.endedAt() == null ? report.startedAt() : report.endedAt()).toInstant().toString());

        JsonArray fields = new JsonArray();
        fields.add(field("Session", "**Server:** " + safe(report.session.server())
            + "\n**Started:** " + LOCAL_TIME.format(report.startedAt())
            + "\n**Ended:** " + (report.endedAt() == null ? "Not finished" : LOCAL_TIME.format(report.endedAt()))
            + "\n**Duration:** " + report.durationText(), false));
        fields.add(field("Inventory", inventorySummary(report), false));
        fields.add(field("Storage", "**Shulkers inspected:** " + report.shulkersInspected()
            + "\n**Pets recorded:** " + report.petsStored()
            + "\n**Fruits recorded:** " + report.fruitsStored()
            + "\n**Item transfers:** " + report.transfers.size(), true));
        fields.add(field("Money", "**Earned:** " + number(report.moneyEarned)
            + "\n**Spent:** " + number(report.moneySpent)
            + "\n**Net:** " + signed(report.netMoney()), true));
        fields.add(field("Combat", "**Mob attacks:** " + report.attacks
            + "\n**Mobs defeated:** " + report.defeats
            + "\n**Deaths:** " + report.deaths
            + "\n**Lowest health:** " + (Double.isNaN(report.lowestHealth) ? "Not recorded" : oneDecimal(report.lowestHealth)), true));
        fields.add(field("Activity", "**Interactions:** " + report.interactions.size()
            + "\n**Recorded events:** " + report.rawRows.size()
            + "\n**Dropped events:** " + report.session.droppedEvents(), true));
        embed.add("fields", fields);

        JsonObject footer = new JsonObject();
        footer.addProperty("text", attachmentReady
            ? "Detailed report attached: " + report.label + ".xlsx"
            : "Detailed workbook unavailable; the session summary was still delivered.");
        embed.add("footer", footer);
        embeds.add(embed);
        payload.add("embeds", embeds);
        return payload.toString();
    }

    static String markAttachmentUnavailable(String payloadJson) {
        try {
            JsonObject payload = com.google.gson.JsonParser.parseString(payloadJson).getAsJsonObject();
            JsonObject embed = payload.getAsJsonArray("embeds").get(0).getAsJsonObject();
            JsonObject footer = new JsonObject();
            footer.addProperty("text", "Detailed workbook unavailable; the session summary was still delivered.");
            embed.add("footer", footer);
            return payload.toString();
        } catch (RuntimeException ignored) {
            return payloadJson;
        }
    }

    private static String inventorySummary(TelemetrySessionReport report) {
        StringBuilder value = new StringBuilder();
        appendItems(value, "Gained", report.topGained(3), report.inventorySummary.stream().filter(row -> row.change() > 0).count());
        appendItems(value, "Lost", report.topLost(3), report.inventorySummary.stream().filter(row -> row.change() < 0).count());
        if (value.isEmpty()) return "No net inventory change.";
        return value.toString().trim();
    }

    private static void appendItems(StringBuilder value, String heading,
                                    List<TelemetrySessionReport.InventorySummaryRow> rows, long totalRows) {
        if (rows.isEmpty()) return;
        if (!value.isEmpty()) value.append('\n');
        value.append("**").append(heading).append(":**\n");
        for (TelemetrySessionReport.InventorySummaryRow row : rows) {
            value.append("• ").append(safe(row.item())).append(" ").append(signed(row.change())).append('\n');
        }
        if (totalRows > rows.size()) value.append("• and ").append(totalRows - rows.size()).append(" more in the workbook\n");
    }

    private static JsonObject field(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value == null || value.isBlank() ? "None" : value.substring(0, Math.min(1_024, value.length())));
        field.addProperty("inline", inline);
        return field;
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replace("@", "＠").replace("`", "'").trim();
    }

    private static String signed(long value) {
        return (value > 0 ? "+" : "") + number(value);
    }

    private static String number(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", value);
    }

    private static String oneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
