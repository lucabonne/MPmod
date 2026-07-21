package net.minepiece.qol.telemetry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dhatim.fastexcel.VisibilityState;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

final class TelemetryWorkbookExporter {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ISO_INSTANT;
    private static final String NAVY = "FF17233C";
    private static final String TEAL = "FF187C7A";
    private static final String LIGHT_TEAL = "FFE5F4F3";
    private static final String LIGHT_BLUE = "FFEEF4FB";
    private static final String GREEN = "FF237A4B";
    private static final String LIGHT_GREEN = "FFE7F5EC";
    private static final String RED = "FFB33A3A";
    private static final String LIGHT_RED = "FFFBEAEA";
    private static final String AMBER = "FFA76513";
    private static final String LIGHT_AMBER = "FFFFF4D6";
    private static final String MUTED = "FF596579";
    private static final String WHITE = "FFFFFFFF";

    private TelemetryWorkbookExporter() {
    }

    static Path export(TelemetryDatabase database, Path exportsDir, String sessionId, boolean discordSafe) throws Exception {
        Files.createDirectories(exportsDir);
        if (sessionId == null) {
            Path output = exportsDir.resolve("all-history.xlsx");
            writeLegacyHistory(output, database.sessions(), database.events(null), true);
            return output;
        }
        TelemetrySessionReport report = TelemetrySessionReport.build(database, sessionId, discordSafe, ZoneId.systemDefault());
        String suffix = discordSafe ? "" : "-full";
        Path output = exportsDir.resolve(report.label + suffix + ".xlsx");
        writeSession(output, report);
        return output;
    }

    private static void writeSession(Path output, TelemetrySessionReport report) throws IOException {
        try (OutputStream stream = Files.newOutputStream(output);
             Workbook workbook = new Workbook(stream, "Minepiece QoL", "02.0000")) {
            writeOverview(workbook.newWorksheet("Overview"), report);
            writeTimeline(workbook.newWorksheet("Timeline"), report);
            writeInventory(workbook.newWorksheet("Inventory"), report);
            writeStorageContents(workbook.newWorksheet("Storage Contents"), report);
            writeTransfers(workbook.newWorksheet("Item Transfers"), report);
            writeEconomy(workbook.newWorksheet("Economy"), report);
            writeCombat(workbook.newWorksheet("Combat"), report);
            writeInteractions(workbook.newWorksheet("Interactions"), report);
            writeRaw(workbook.newWorksheet("Raw Data"), report);
        }
    }

    private static void writeOverview(Worksheet sheet, TelemetrySessionReport report) {
        baseSheet(sheet, 100);
        sheet.value(0, 0, report.label);
        sheet.value(1, 0, "Minepiece session report — readable summary and detailed activity log");
        title(sheet, 0, 0, 0, 5);
        sheet.range(1, 0, 1, 5).style().fontColor(MUTED).italic().set();
        sheet.rowHeight(0, 29);

        section(sheet, 3, "Session details", 5);
        keyValue(sheet, 4, 0, "Server", report.session.server());
        keyValue(sheet, 4, 3, "Status", report.session.cleanEnd() ? "Completed normally" : "Incomplete session");
        keyValue(sheet, 5, 0, "Started", report.startedAt().toLocalDateTime());
        keyValue(sheet, 5, 3, "Ended", report.endedAt() == null ? "Not finished" : report.endedAt().toLocalDateTime());
        keyValue(sheet, 6, 0, "Duration", report.durationText());
        keyValue(sheet, 6, 3, "Report file", report.label + ".xlsx");
        keyValue(sheet, 7, 0, "Client version", report.session.clientVersion());
        keyValue(sheet, 7, 3, "Mod version", report.session.modVersion());
        sheet.range(5, 1, 5, 1).style().format("yyyy-mm-dd hh:mm:ss").set();
        if (report.endedAt() != null) sheet.range(5, 4, 5, 4).style().format("yyyy-mm-dd hh:mm:ss").set();
        if (!report.session.cleanEnd()) sheet.range(4, 4, 4, 4).style().fillColor(LIGHT_AMBER).fontColor(AMBER).bold().set();

        section(sheet, 9, "At a glance", 5);
        metric(sheet, 10, 0, "Money earned", report.moneyEarned, LIGHT_GREEN, GREEN);
        metric(sheet, 10, 2, "Money spent", report.moneySpent, LIGHT_RED, RED);
        metric(sheet, 10, 4, "Net money", report.netMoney(), report.netMoney() >= 0 ? LIGHT_GREEN : LIGHT_RED,
            report.netMoney() >= 0 ? GREEN : RED);
        metric(sheet, 12, 0, "Items gained", report.itemsGained(), LIGHT_GREEN, GREEN);
        metric(sheet, 12, 2, "Items lost", report.itemsLost(), LIGHT_RED, RED);
        metric(sheet, 12, 4, "Interactions", report.interactions.size(), LIGHT_BLUE, TEAL);
        metric(sheet, 14, 0, "Mob attacks", report.attacks, LIGHT_BLUE, TEAL);
        metric(sheet, 14, 2, "Mobs defeated", report.defeats, LIGHT_GREEN, GREEN);
        metric(sheet, 14, 4, "Deaths", report.deaths, report.deaths == 0 ? LIGHT_GREEN : LIGHT_RED,
            report.deaths == 0 ? GREEN : RED);

        section(sheet, 17, "Top item changes", 5);
        writeRow(sheet, 18, new Object[]{"Gained", "Quantity", "", "Lost", "Quantity", ""});
        header(sheet, 18, 5);
        List<TelemetrySessionReport.InventorySummaryRow> gained = report.topGained(5);
        List<TelemetrySessionReport.InventorySummaryRow> lost = report.topLost(5);
        int itemRows = Math.max(1, Math.max(gained.size(), lost.size()));
        for (int i = 0; i < itemRows; i++) {
            if (i < gained.size()) {
                sheet.value(19 + i, 0, safe(gained.get(i).item()));
                sheet.value(19 + i, 1, gained.get(i).change());
                sheet.range(19 + i, 1, 19 + i, 1).style().fontColor(GREEN).format("+0;-0;0").set();
            } else if (i == 0) {
                sheet.value(19, 0, "No items gained");
            }
            if (i < lost.size()) {
                sheet.value(19 + i, 3, safe(lost.get(i).item()));
                sheet.value(19 + i, 4, lost.get(i).change());
                sheet.range(19 + i, 4, 19 + i, 4).style().fontColor(RED).format("+0;-0;0").set();
            } else if (i == 0) {
                sheet.value(19, 3, "No items lost");
            }
        }

        int qualityRow = 20 + itemRows;
        section(sheet, qualityRow, "Report quality", 5);
        keyValue(sheet, qualityRow + 1, 0, "Recorded events", report.rawRows.size());
        keyValue(sheet, qualityRow + 1, 3, "Dropped events", report.session.droppedEvents());
        keyValue(sheet, qualityRow + 2, 0, "Raw technical data", "Available in the hidden Raw Data sheet");
        if (report.session.droppedEvents() > 0L) {
            sheet.range(qualityRow + 1, 4, qualityRow + 1, 4).style().fillColor(LIGHT_AMBER).fontColor(AMBER).bold().set();
        }

        sheet.width(0, 25);
        sheet.width(1, 18);
        sheet.width(2, 4);
        sheet.width(3, 25);
        sheet.width(4, 22);
        sheet.width(5, 4);
    }

    private static void writeTimeline(Worksheet sheet, TelemetrySessionReport report) {
        prepareTableSheet(sheet, "Readable timeline", "Technical inventory snapshots and chat data are kept out of this view.",
            new String[]{"Time", "Category", "Activity", "Details"}, new double[]{21, 16, 28, 60});
        int row = 3;
        for (TelemetrySessionReport.TimelineRow value : report.timeline) {
            writeRow(sheet, row++, new Object[]{value.time().toLocalDateTime(), value.category(), value.activity(), value.details()});
        }
        finishTable(sheet, 2, row - 1, 3, 0);
    }

    private static void writeInventory(Worksheet sheet, TelemetrySessionReport report) {
        baseSheet(sheet, 95);
        sheet.value(0, 0, "Inventory");
        sheet.value(1, 0, "Starting and ending player inventory, followed by actual item changes.");
        title(sheet, 0, 0, 0, 7);
        sheet.range(1, 0, 1, 7).style().fontColor(MUTED).italic().set();
        writeRow(sheet, 2, new Object[]{"Item", "Starting quantity", "Ending quantity", "Net change"});
        header(sheet, 2, 3);
        int row = 3;
        if (report.inventorySummary.isEmpty()) {
            sheet.value(row++, 0, "No non-empty player inventory was recorded");
        } else {
            for (TelemetrySessionReport.InventorySummaryRow value : report.inventorySummary) {
                writeRow(sheet, row, new Object[]{value.item(), value.starting(), value.ending(), value.change()});
                styleSigned(sheet, row, 3, value.change());
                row++;
            }
        }
        int logTitle = row + 2;
        section(sheet, logTitle, "Change log", 7);
        int headerRow = logTitle + 1;
        writeRow(sheet, headerRow, new Object[]{"Time", "Location", "Action", "Item", "Previous", "New", "Change", "Slot"});
        header(sheet, headerRow, 7);
        row = headerRow + 1;
        if (report.inventoryChanges.isEmpty()) {
            sheet.value(row++, 0, "No item changes occurred during this session");
        } else {
            for (TelemetrySessionReport.InventoryChangeRow value : report.inventoryChanges) {
                writeRow(sheet, row, new Object[]{value.time().toLocalDateTime(), value.location(), value.action(), value.item(),
                    value.previousQuantity(), value.newQuantity(), value.change(), value.slot()});
                styleSigned(sheet, row, 6, value.change());
                row++;
            }
        }
        sheet.freezePane(0, 3);
        sheet.setAutoFilter(2, 0, Math.max(2, logTitle - 3), 3);
        sheet.range(3, 0, Math.max(3, logTitle - 3), 3).style().shadeAlternateRows(LIGHT_BLUE).set();
        if (row > headerRow + 1) sheet.range(headerRow + 1, 0, row - 1, 7).style().shadeAlternateRows(LIGHT_BLUE).set();
        sheet.range(headerRow + 1, 0, Math.max(headerRow + 1, row - 1), 0).style().format("yyyy-mm-dd hh:mm:ss").set();
        double[] widths = {30, 22, 24, 32, 12, 12, 12, 10};
        setWidths(sheet, widths);
    }

    private static void writeEconomy(Worksheet sheet, TelemetrySessionReport report) {
        prepareTableSheet(sheet, "Money", "Individual transactions are logged here; Discord receives only final totals.",
            new String[]{"Time", "Direction", "Amount", "Source", "Description"}, new double[]{21, 18, 14, 25, 55});
        int row = 3;
        for (TelemetrySessionReport.EconomyRow value : report.economy) {
            writeRow(sheet, row, new Object[]{value.time().toLocalDateTime(), value.direction(), value.amount(), value.source(), value.description()});
            sheet.range(row, 2, row, 2).style().fontColor("Money earned".equals(value.direction()) ? GREEN : RED).format("#,##0").set();
            row++;
        }
        if (report.economy.isEmpty()) sheet.value(row++, 0, "No money changes were recorded");
        int totalRow = row + 1;
        writeRow(sheet, totalRow, new Object[]{"Session totals", "Earned", report.moneyEarned, "Spent", report.moneySpent});
        sheet.range(totalRow, 0, totalRow, 4).style().bold().fillColor(LIGHT_TEAL).set();
        writeRow(sheet, totalRow + 1, new Object[]{"", "Net money", report.netMoney()});
        styleSigned(sheet, totalRow + 1, 2, report.netMoney());
        finishTable(sheet, 2, row - 1, 4, 0);
    }

    private static void writeStorageContents(Worksheet sheet, TelemetrySessionReport report) {
        baseSheet(sheet, 95);
        sheet.value(0, 0, "Storage Contents");
        sheet.value(1, 0, "Latest known item locations. Shulker contents are indented beneath their box; unchanged storage is not repeated.");
        title(sheet, 0, 0, 0, 6);
        sheet.range(1, 0, 1, 6).style().fontColor(MUTED).italic().set();
        writeRow(sheet, 2, new Object[]{"Location", "Position", "Item", "Quantity", "Status"});
        header(sheet, 2, 4);
        int row = 3;
        for (TelemetrySessionReport.StorageContentRow value : report.storageContents) {
            writeRow(sheet, row, new Object[]{value.location(), value.slot(), value.item(), value.quantity(), value.status()});
            if (value.shulker() && value.depth() == 0) {
                sheet.range(row, 0, row, 4).style().fillColor(LIGHT_TEAL).bold().set();
            } else if (value.depth() > 0) {
                sheet.range(row, 1, row, 4).style().fontColor(MUTED).set();
            }
            row++;
        }
        if (report.storageContents.isEmpty()) sheet.value(row++, 0, "No storage was inspected during this session");
        int logTitle = row + 2;
        section(sheet, logTitle, "What changed", 6);
        int headerRow = logTitle + 1;
        writeRow(sheet, headerRow, new Object[]{"Time", "Location", "Position", "Change", "Item", "Quantity", "Details"});
        header(sheet, headerRow, 6);
        row = headerRow + 1;
        for (TelemetrySessionReport.StorageChangeRow value : report.storageChanges) {
            writeRow(sheet, row, new Object[]{value.time().toLocalDateTime(), value.location(), value.slot(), value.action(),
                value.item(), value.quantity(), value.details()});
            sheet.range(row, 3, row, 3).style().fontColor("Added".equals(value.action()) ? GREEN : RED).set();
            row++;
        }
        if (report.storageChanges.isEmpty()) sheet.value(row++, 0, "No storage contents changed during this session");
        sheet.freezePane(0, 3);
        sheet.setAutoFilter(2, 0, Math.max(2, logTitle - 3), 4);
        sheet.range(3, 0, Math.max(3, logTitle - 3), 4).style().shadeAlternateRows(LIGHT_BLUE).set();
        if (row > headerRow + 1) sheet.range(headerRow + 1, 0, row - 1, 6).style().shadeAlternateRows(LIGHT_BLUE).set();
        sheet.range(headerRow + 1, 0, Math.max(headerRow + 1, row - 1), 0).style().format("yyyy-mm-dd hh:mm:ss").set();
        setWidths(sheet, new double[]{25, 24, 34, 12, 38, 12, 52});
    }

    private static void writeTransfers(Worksheet sheet, TelemetrySessionReport report) {
        prepareTableSheet(sheet, "Item Transfers",
            "Only detected movements are listed. Unconfirmed sources or destinations are labelled instead of guessed.",
            new String[]{"Time", "Item", "Quantity", "From", "To", "Result"},
            new double[]{21, 34, 12, 42, 42, 16});
        int row = 3;
        for (TelemetrySessionReport.TransferRow value : report.transfers) {
            writeRow(sheet, row, new Object[]{value.time().toLocalDateTime(), value.item(), value.quantity(),
                value.from(), value.to(), value.result()});
            if ("Discarded".equals(value.result()) || "Dropped".equals(value.result())) {
                sheet.range(row, 5, row, 5).style().fontColor(RED).bold().set();
            } else if ("Received".equals(value.result())) {
                sheet.range(row, 5, row, 5).style().fontColor(GREEN).set();
            }
            row++;
        }
        if (report.transfers.isEmpty()) sheet.value(row++, 0, "No item transfers were detected");
        finishTable(sheet, 2, row - 1, 5, 0);
    }

    private static void writeCombat(Worksheet sheet, TelemetrySessionReport report) {
        prepareTableSheet(sheet, "Combat", "Mob activity and health changes in plain language.",
            new String[]{"Time", "Event", "Target", "Health before", "Health after", "Change"},
            new double[]{21, 22, 34, 15, 15, 13});
        int row = 3;
        for (TelemetrySessionReport.CombatRow value : report.combat) {
            writeRow(sheet, row, new Object[]{value.time().toLocalDateTime(), value.event(), value.target(),
                value.healthBefore(), value.healthAfter(), value.change()});
            if (value.change() != null) styleSigned(sheet, row, 5, value.change());
            row++;
        }
        if (report.combat.isEmpty()) sheet.value(row++, 0, "No combat events were recorded");
        finishTable(sheet, 2, row - 1, 5, 0);
        if (row > 3) sheet.range(3, 3, row - 1, 5).style().format("0.0").set();
    }

    private static void writeInteractions(Worksheet sheet, TelemetrySessionReport report) {
        prepareTableSheet(sheet, "Interactions", "Items, blocks, mobs, and inventory actions with friendly names.",
            new String[]{"Time", "Action", "Target", "Hand", "Slot"}, new double[]{21, 28, 42, 16, 10});
        int row = 3;
        for (TelemetrySessionReport.InteractionRow value : report.interactions) {
            writeRow(sheet, row++, new Object[]{value.time().toLocalDateTime(), value.action(), value.target(), value.hand(), value.slot()});
        }
        if (report.interactions.isEmpty()) sheet.value(row++, 0, "No interactions were recorded");
        finishTable(sheet, 2, row - 1, 4, 0);
    }

    private static void writeRaw(Worksheet sheet, TelemetrySessionReport report) {
        sheet.setVisibilityState(VisibilityState.HIDDEN);
        writeRow(sheet, 0, new Object[]{"Session ID", "Sequence", "Local timestamp", "Category code", "Type code", "Raw JSON"});
        header(sheet, 0, 5);
        int row = 1;
        for (TelemetrySessionReport.RawRow value : report.rawRows) {
            writeRow(sheet, row++, new Object[]{value.sessionId(), value.sequence(), value.time().toLocalDateTime(),
                value.category(), value.type(), value.json()});
        }
        sheet.freezePane(0, 1);
        sheet.setAutoFilter(0, 0, Math.max(0, row - 1), 5);
        sheet.range(1, 2, Math.max(1, row - 1), 2).style().format("yyyy-mm-dd hh:mm:ss").set();
        setWidths(sheet, new double[]{38, 11, 21, 18, 24, 100});
    }

    private static void prepareTableSheet(Worksheet sheet, String title, String subtitle, String[] headers, double[] widths) {
        baseSheet(sheet, 95);
        sheet.value(0, 0, title);
        sheet.value(1, 0, subtitle);
        title(sheet, 0, 0, 0, headers.length - 1);
        sheet.range(1, 0, 1, headers.length - 1).style().fontColor(MUTED).italic().set();
        writeRow(sheet, 2, headers);
        header(sheet, 2, headers.length - 1);
        setWidths(sheet, widths);
    }

    private static void finishTable(Worksheet sheet, int headerRow, int lastRow, int lastColumn, int timeColumn) {
        int end = Math.max(headerRow, lastRow);
        sheet.freezePane(0, headerRow + 1);
        sheet.setAutoFilter(headerRow, 0, end, lastColumn);
        if (lastRow > headerRow) {
            sheet.range(headerRow + 1, 0, lastRow, lastColumn).style().shadeAlternateRows(LIGHT_BLUE).set();
            sheet.range(headerRow + 1, timeColumn, lastRow, timeColumn).style().format("yyyy-mm-dd hh:mm:ss").set();
        }
    }

    private static void baseSheet(Worksheet sheet, int zoom) {
        sheet.hideGridLines();
        sheet.setZoom(zoom);
    }

    private static void title(Worksheet sheet, int top, int left, int bottom, int right) {
        sheet.range(top, left, bottom, right).style().fillColor(NAVY).fontColor(WHITE).bold().fontSize(16)
            .verticalAlignment("center").set();
    }

    private static void section(Worksheet sheet, int row, String value, int lastColumn) {
        sheet.value(row, 0, value);
        sheet.range(row, 0, row, lastColumn).style().fillColor(TEAL).fontColor(WHITE).bold().set();
        sheet.rowHeight(row, 21);
    }

    private static void header(Worksheet sheet, int row, int lastColumn) {
        sheet.range(row, 0, row, lastColumn).style().fillColor(NAVY).fontColor(WHITE).bold()
            .verticalAlignment("center").wrapText(true).set();
        sheet.rowHeight(row, 22);
    }

    private static void keyValue(Worksheet sheet, int row, int column, String key, Object value) {
        sheet.value(row, column, key);
        writeCell(sheet, row, column + 1, value);
        sheet.range(row, column, row, column).style().fontColor(MUTED).bold().set();
        sheet.range(row, column + 1, row, column + 1).style().fillColor(LIGHT_BLUE).set();
    }

    private static void metric(Worksheet sheet, int row, int column, String label, Number value, String fill, String font) {
        sheet.value(row, column, label);
        sheet.value(row + 1, column, value);
        sheet.range(row, column, row + 1, column + 1).style().fillColor(fill).borderStyle("thin").borderColor("FFD5DCE6").set();
        sheet.range(row, column, row, column + 1).style().fontColor(MUTED).bold().set();
        sheet.range(row + 1, column, row + 1, column + 1).style().fontColor(font).bold().fontSize(15).format("#,##0").set();
        sheet.rowHeight(row + 1, 24);
    }

    private static void styleSigned(Worksheet sheet, int row, int column, Number value) {
        double numeric = value.doubleValue();
        String color = numeric > 0D ? GREEN : numeric < 0D ? RED : MUTED;
        sheet.range(row, column, row, column).style().fontColor(color).bold().format("+0;-0;0").set();
    }

    private static void setWidths(Worksheet sheet, double[] widths) {
        for (int column = 0; column < widths.length; column++) sheet.width(column, widths[column]);
    }

    private static void writeRow(Worksheet sheet, int row, Object[] values) {
        for (int column = 0; column < values.length; column++) writeCell(sheet, row, column, values[column]);
    }

    private static void writeCell(Worksheet sheet, int row, int column, Object value) {
        if (value == null) return;
        if (value instanceof String text) {
            sheet.value(row, column, safe(text));
        } else if (value instanceof Number number) {
            sheet.value(row, column, number);
        } else if (value instanceof Boolean bool) {
            sheet.value(row, column, bool);
        } else if (value instanceof java.time.LocalDateTime dateTime) {
            sheet.value(row, column, dateTime);
        } else {
            sheet.value(row, column, safe(value.toString()));
        }
    }

    private static String safe(String value) {
        return TelemetryRedactor.excelSafe(value == null ? "" : value);
    }

    private static void writeLegacyHistory(Path output, List<TelemetryDatabase.SessionRecord> sessions,
                                           List<TelemetryDatabase.StoredEvent> events, boolean includeChatCommands) throws IOException {
        try (OutputStream stream = Files.newOutputStream(output);
             Workbook workbook = new Workbook(stream, "Minepiece QoL", "01.0100")) {
            writeLegacySummary(workbook.newWorksheet("Summary"), sessions, events);
            List<TelemetryDatabase.StoredEvent> visibleEvents = includeChatCommands
                ? events : events.stream().filter(event -> !"CHAT_COMMAND".equals(event.category())).toList();
            writeLegacyEvents(workbook.newWorksheet("Timeline"), visibleEvents);
            writeLegacyFiltered(workbook.newWorksheet("Inventory"), events, "INVENTORY");
            writeLegacyFiltered(workbook.newWorksheet("Economy"), events, "ECONOMY");
            writeLegacyFiltered(workbook.newWorksheet("Combat"), events, "COMBAT");
            if (includeChatCommands) writeLegacyFiltered(workbook.newWorksheet("ChatCommands"), events, "CHAT_COMMAND");
            writeLegacyFiltered(workbook.newWorksheet("Interactions"), events, "INTERACTION");
        }
    }

    private static void writeLegacySummary(Worksheet sheet, List<TelemetryDatabase.SessionRecord> sessions,
                                           List<TelemetryDatabase.StoredEvent> events) {
        writeLegacyRow(sheet, 0, new String[]{"Session ID", "Started", "Ended", "Server", "Client", "Mod", "Clean end", "Dropped events"});
        int row = 1;
        for (TelemetryDatabase.SessionRecord session : sessions) {
            writeLegacyRow(sheet, row++, new String[]{session.id(), instant(session.startedMs()),
                session.endedMs() == null ? "" : instant(session.endedMs()), session.server(), session.clientVersion(),
                session.modVersion(), Boolean.toString(session.cleanEnd()), Long.toString(session.droppedEvents())});
        }
        row += 2;
        writeLegacyRow(sheet, row++, new String[]{"Category", "Events"});
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TelemetryDatabase.StoredEvent event : events) counts.merge(event.category(), 1, Integer::sum);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            writeLegacyRow(sheet, row++, new String[]{entry.getKey(), Integer.toString(entry.getValue())});
        }
    }

    private static void writeLegacyEvents(Worksheet sheet, List<TelemetryDatabase.StoredEvent> events) {
        writeLegacyRow(sheet, 0, new String[]{"Session ID", "Sequence", "Timestamp", "Category", "Type", "Details"});
        int row = 1;
        for (TelemetryDatabase.StoredEvent event : events) writeLegacyRow(sheet, row++, legacyEventRow(event));
    }

    private static void writeLegacyFiltered(Worksheet sheet, List<TelemetryDatabase.StoredEvent> events, String category) {
        writeLegacyRow(sheet, 0, new String[]{"Session ID", "Sequence", "Timestamp", "Category", "Type", "Details"});
        int row = 1;
        for (TelemetryDatabase.StoredEvent event : events) {
            if (category.equals(event.category())) writeLegacyRow(sheet, row++, legacyEventRow(event));
        }
    }

    private static String[] legacyEventRow(TelemetryDatabase.StoredEvent event) {
        return new String[]{event.sessionId(), Long.toString(event.sequence()), instant(event.timestampMs()),
            event.category(), event.type(), flattenJson(event.dataJson())};
    }

    private static void writeLegacyRow(Worksheet sheet, int row, String[] values) {
        for (int column = 0; column < values.length; column++) sheet.value(row, column, safe(values[column]));
    }

    private static String flattenJson(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) return parsed.toString();
            JsonObject object = parsed.getAsJsonObject();
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                parts.add(entry.getKey() + "=" + (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                    ? value.getAsString() : value.toString()));
            }
            return String.join("; ", parts);
        } catch (RuntimeException ignored) {
            return json;
        }
    }

    private static String instant(long epochMs) {
        return DISPLAY_TIME.format(Instant.ofEpochMilli(epochMs));
    }
}
