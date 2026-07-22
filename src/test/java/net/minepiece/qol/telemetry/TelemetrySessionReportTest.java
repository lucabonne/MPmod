package net.minepiece.qol.telemetry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetrySessionReportTest {
    @TempDir
    Path tempDir;

    @Test
    void buildsFriendlyReportAndDailySessionNumber() throws Exception {
        long firstStart = Instant.parse("2026-07-20T09:00:00Z").toEpochMilli();
        long secondStart = Instant.parse("2026-07-20T10:00:00Z").toEpochMilli();
        try (TelemetryDatabase database = new TelemetryDatabase(this.tempDir.resolve("history.sqlite"))) {
            database.startSession("s1", firstStart, "play.minepiece.net", "1.21.11", "1.1.0");
            database.finishSession("s1", firstStart + 60_000L, true, 0L);
            database.startSession("s2", secondStart, "play.minepiece.net", "1.21.11", "1.1.0");
            database.insertEvents(List.of(
                event("s2", 0, secondStart + 500L, TelemetryEvent.Category.SESSION, "session_start",
                    "{\"server\":\"play.minepiece.net\",\"username\":\"TestPlayer\"}"),
                event("s2", 1, secondStart + 1_000L, TelemetryEvent.Category.INVENTORY, "slot_snapshot",
                    "{\"item_id\":\"minecraft:netherite_sword\",\"name\":\"Big Mom's Napoleon\",\"count\":1,\"inventory\":\"player\",\"reason\":\"session_start\",\"slot\":0,\"previous_count\":0}"),
                event("s2", 2, secondStart + 2_000L, TelemetryEvent.Category.INVENTORY, "slot_change",
                    "{\"item_id\":\"minecraft:netherite_sword\",\"name\":\"Big Mom's Napoleon\",\"count\":2,\"inventory\":\"player\",\"reason\":\"periodic\",\"slot\":0,\"previous_count\":1,\"previous_item_id\":\"minecraft:netherite_sword\",\"previous_name\":\"Big Mom's Napoleon\"}"),
                event("s2", 3, secondStart + 3_000L, TelemetryEvent.Category.ECONOMY, "money_change",
                    "{\"kind\":\"income\",\"source\":\"actionbar\",\"signed_amount\":100}"),
                event("s2", 4, secondStart + 4_000L, TelemetryEvent.Category.ECONOMY, "money_change",
                    "{\"kind\":\"expense\",\"source\":\"actionbar\",\"signed_amount\":-30}"),
                event("s2", 5, secondStart + 5_000L, TelemetryEvent.Category.COMBAT, "attack_attempt",
                    "{\"target_type\":\"minecraft:zombie\",\"target_name\":\"Zombie\"}"),
                event("s2", 6, secondStart + 6_000L, TelemetryEvent.Category.INTERACTION, "use_item",
                    "{\"hand\":\"MAIN_HAND\",\"item\":\"minecraft:netherite_sword\",\"item_name\":\"Big Mom's Napoleon\"}")
            ));
            database.finishSession("s2", secondStart + 120_000L, true, 0L);

            TelemetrySessionReport report = TelemetrySessionReport.build(database, "s2", true, ZoneId.of("Europe/Rome"));

            assertEquals("TestPlayer-2026.07.20-2", report.label);
            assertEquals(100L, report.moneyEarned);
            assertEquals(30L, report.moneySpent);
            assertEquals(70L, report.netMoney());
            assertEquals(1, report.itemsGained());
            assertEquals("Big Mom's Napoleon", report.inventorySummary.getFirst().item());
            assertEquals("Mob — Zombie", report.combat.getFirst().target());
            assertEquals("Used item", report.interactions.getFirst().action());
            assertEquals("Main hand", report.interactions.getFirst().hand());

            JsonObject payload = JsonParser.parseString(TelemetryDiscordFormatter.sessionPayload(report, true)).getAsJsonObject();
            JsonObject embed = payload.getAsJsonArray("embeds").get(0).getAsJsonObject();
            assertEquals("TestPlayer-2026.07.20-2 — Session complete", embed.get("title").getAsString());
            assertTrue(embed.toString().contains("Big Mom's Napoleon"));
            assertTrue(embed.toString().contains("Earned"));
            assertFalse(embed.toString().contains("minecraft:"));
            assertTrue(TelemetryDiscordFormatter.markAttachmentUnavailable(payload.toString()).contains("workbook unavailable"));
        }
    }

    @Test
    void buildsCompactReadableSummariesWithoutRoutineHealthChanges() throws Exception {
        long start = Instant.parse("2026-07-20T12:00:00Z").toEpochMilli();
        try (TelemetryDatabase database = new TelemetryDatabase(this.tempDir.resolve("compact.sqlite"))) {
            database.startSession("compact", start, "play.minepiece.net", "1.21.11", "1.1.0");
            database.insertEvents(List.of(
                event("compact", 1, start + 1_000L, TelemetryEvent.Category.COMBAT, "vitals_change",
                    "{\"previous_health\":20.0,\"health\":12.0}"),
                event("compact", 2, start + 2_000L, TelemetryEvent.Category.COMBAT, "attack_attempt",
                    "{\"target_type\":\"minecraft:zombie\",\"target_name\":\"Zombie\"}"),
                event("compact", 3, start + 3_000L, TelemetryEvent.Category.COMBAT, "attack_attempt",
                    "{\"target_type\":\"minecraft:zombie\",\"target_name\":\"Zombie\"}"),
                event("compact", 4, start + 4_000L, TelemetryEvent.Category.COMBAT, "death", "{}"),
                event("compact", 5, start + 5_000L, TelemetryEvent.Category.ECONOMY, "money_change",
                    "{\"source\":\"actionbar\",\"signed_amount\":100}"),
                event("compact", 6, start + 6_000L, TelemetryEvent.Category.ECONOMY, "money_change",
                    "{\"source\":\"actionbar\",\"signed_amount\":50}"),
                event("compact", 7, start + 7_000L, TelemetryEvent.Category.INVENTORY, "slot_change",
                    "{\"item_id\":\"minecraft:apple\",\"name\":\"Apple\",\"count\":2,"
                        + "\"previous_item_id\":\"minecraft:air\",\"previous_name\":\"\",\"previous_count\":0,"
                        + "\"inventory\":\"ender_chest\",\"inventory_name\":\"Ender Chest\",\"slot\":0}"),
                event("compact", 8, start + 8_000L, TelemetryEvent.Category.INVENTORY, "slot_change",
                    "{\"item_id\":\"minecraft:apple\",\"name\":\"Apple\",\"count\":1,"
                        + "\"previous_item_id\":\"minecraft:apple\",\"previous_name\":\"Apple\",\"previous_count\":2,"
                        + "\"inventory\":\"ender_chest\",\"inventory_name\":\"Ender Chest\",\"slot\":0}"),
                event("compact", 9, start + 9_000L, TelemetryEvent.Category.INTERACTION, "inventory_click",
                    "{\"action\":\"PICKUP\",\"slot\":4}"),
                event("compact", 10, start + 10_000L, TelemetryEvent.Category.INTERACTION, "inventory_click",
                    "{\"action\":\"PICKUP\",\"slot\":9}")
            ));
            database.finishSession("compact", start + 60_000L, true, 0L);

            TelemetrySessionReport report = TelemetrySessionReport.build(database, "compact", true,
                ZoneId.of("Europe/Rome"));

            assertFalse(report.timelineSummary().stream().anyMatch(row -> row.summary().contains("Health changed")));
            TelemetrySessionReport.TimelineSummaryRow combatMinute = report.timelineSummary().stream()
                .filter(row -> row.category().equals("Combat")).findFirst().orElseThrow();
            assertEquals(3, combatMinute.events());
            assertTrue(combatMinute.summary().contains("Mob attacked x2"));
            assertTrue(combatMinute.summary().contains("Player died"));

            assertEquals(2, report.combatSummary().stream()
                .filter(row -> row.event().equals("Mob attacked")).findFirst().orElseThrow().count());
            assertTrue(report.combatSummary().stream().anyMatch(row -> row.event().equals("Player died")));

            TelemetrySessionReport.EconomySummaryRow economy = report.economySummary().getFirst();
            assertEquals(2, economy.transactions());
            assertEquals(150L, economy.total());
            assertEquals(75D, economy.average());
            assertEquals(50L, economy.minimum());
            assertEquals(100L, economy.maximum());

            TelemetrySessionReport.StorageItemSummaryRow stored = report.storageItemSummary().getFirst();
            assertEquals("Ender Chest", stored.location());
            assertEquals("Apple", stored.item());
            assertEquals(1, stored.quantity());
            TelemetrySessionReport.StorageChangeSummaryRow storageChange = report.storageChangeSummary().getFirst();
            assertEquals(2, storageChange.added());
            assertEquals(1, storageChange.removed());
            assertEquals(1, storageChange.net());

            TelemetrySessionReport.TransferSummaryRow transfer = report.transferSummary().getFirst();
            assertEquals(1L, transfer.received());
            assertEquals(1L, transfer.moved());
            assertEquals(1L, transfer.net());

            TelemetrySessionReport.InteractionSummaryRow interaction = report.interactionSummary().getFirst();
            assertEquals("Inventory slots", interaction.target());
            assertEquals(2, interaction.count());
        }
    }

    @Test
    void humanizesTechnicalFallbacks() {
        assertEquals("Netherite Sword", TelemetrySessionReport.friendlyItem("", "minecraft:netherite_sword"));
        assertEquals("Mob", TelemetrySessionReport.friendlyEntity("Interaction", "minecraft:interaction"));
        assertEquals("Mob — Zombie", TelemetrySessionReport.friendlyEntity("", "minecraft:zombie"));
    }

    @Test
    void reportsNestedStorageAndDestinationAwareTransfers() throws Exception {
        long start = Instant.parse("2026-07-20T12:00:00Z").toEpochMilli();
        try (TelemetryDatabase database = new TelemetryDatabase(this.tempDir.resolve("storage.sqlite"))) {
            database.startSession("storage", start, "play.minepiece.net", "1.21.11", "1.1.0");
            database.insertEvents(List.of(
                event("storage", 1, start + 1_000L, TelemetryEvent.Category.INVENTORY, "slot_snapshot", """
                    {"item_id":"minecraft:blue_shulker_box","name":"Fisherman","count":1,"signature":"box-a",
                     "container_contents_known":true,"contents":[{"slot":1,"item_id":"minecraft:apple","name":"Apple","count":3}],
                     "inventory":"player","reason":"session_start","slot":0,"previous_count":0,"previous_contents":[]}
                    """),
                event("storage", 2, start + 2_000L, TelemetryEvent.Category.INVENTORY, "slot_change", """
                    {"item_id":"minecraft:blue_shulker_box","name":"Fisherman","count":1,"signature":"box-b",
                     "container_contents_known":true,"contents":[{"slot":1,"item_id":"minecraft:apple","name":"Apple","count":2}],
                     "inventory":"player","reason":"periodic","slot":0,"previous_count":1,
                     "previous_item_id":"minecraft:blue_shulker_box","previous_name":"Fisherman","previous_signature":"box-a",
                     "previous_container_contents_known":true,
                     "previous_contents":[{"slot":1,"item_id":"minecraft:apple","name":"Apple","count":3}]}
                    """),
                event("storage", 3, start + 3_000L, TelemetryEvent.Category.INVENTORY, "slot_change", """
                    {"item_id":"minecraft:air","name":"","count":0,"contents":[],"inventory":"player","reason":"periodic","slot":9,
                     "previous_count":4,"previous_item_id":"minecraft:cobblestone","previous_name":"Cobblestone","previous_signature":"stone",
                     "previous_contents":[],"context_inventory":"trash","context_name":"Trash","interaction_action":"PICKUP"}
                    """),
                event("storage", 4, start + 4_000L, TelemetryEvent.Category.INVENTORY, "slot_snapshot", """
                    {"item_id":"minecraft:wolf_spawn_egg","name":"Chopper","count":1,"inventory":"pet_storage","inventory_name":"Pet Storage",
                     "reason":"container_open","slot":0,"previous_count":0,"contents":[],"previous_contents":[]}
                    """),
                event("storage", 5, start + 5_000L, TelemetryEvent.Category.INVENTORY, "slot_snapshot", """
                    {"item_id":"minecraft:archer_pottery_sherd","name":"Scalpel Fruit","count":1,"inventory":"fruit_storage","inventory_name":"Fruit Storage",
                     "reason":"container_open","slot":4,"previous_count":0,"contents":[],"previous_contents":[]}
                    """),
                event("storage", 6, start + 6_000L, TelemetryEvent.Category.INVENTORY, "slot_change", """
                    {"item_id":"minecraft:air","name":"","count":0,"inventory":"player","reason":"periodic","slot":10,
                     "previous_count":1,"previous_item_id":"minecraft:diamond","previous_name":"Diamond","previous_contents":[],"contents":[],
                     "context_inventory":"ender_chest","context_name":"Ender Chest"}
                    """),
                event("storage", 7, start + 6_050L, TelemetryEvent.Category.INVENTORY, "slot_change", """
                    {"item_id":"minecraft:diamond","name":"Diamond","count":1,"inventory":"ender_chest","inventory_name":"Ender Chest",
                     "reason":"container_change","slot":0,"previous_count":0,"previous_item_id":"minecraft:air","previous_name":"","previous_contents":[],"contents":[]}
                    """)));
            database.finishSession("storage", start + 60_000L, true, 0L);

            TelemetrySessionReport report = TelemetrySessionReport.build(database, "storage", true, ZoneId.of("Europe/Rome"));

            assertEquals(1, report.shulkersInspected());
            assertEquals(1, report.petsStored());
            assertEquals(1, report.fruitsStored());
            assertTrue(report.storageContents.stream().anyMatch(row -> row.item().contains("Fisherman (Shulker #1)")
                && row.slot().equals("Hotbar slot 1")));
            assertTrue(report.storageContents.stream().anyMatch(row -> row.item().equals("Apple")
                && row.slot().equals("↳ Shulker slot 2") && row.quantity() == 2));
            assertTrue(report.storageChanges.stream().anyMatch(row -> row.item().equals("Apple")
                && row.action().equals("Removed") && row.quantity() == 1));
            assertTrue(report.transfers.stream().anyMatch(row -> row.item().equals("Cobblestone")
                && row.from().contains("Inventory slot 1") && row.to().equals("Trash") && row.result().equals("Discarded")));
            assertTrue(report.transfers.stream().anyMatch(row -> row.item().equals("Diamond")
                && row.from().contains("Inventory slot 2") && row.to().contains("Ender Chest — Slot 1")
                && row.result().equals("Moved")));
            assertTrue(TelemetryDiscordFormatter.sessionPayload(report, true).contains("Shulkers inspected"));
        }
    }

    private static TelemetryEvent event(String sessionId, long sequence, long timestamp,
                                        TelemetryEvent.Category category, String type, String json) {
        return new TelemetryEvent(sessionId, sequence, timestamp, category, type, json);
    }
}
