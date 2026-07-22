package net.minepiece.qol.telemetry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class TelemetrySessionReport {
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> GENERIC_ENTITY_NAMES = Set.of("", "interaction", "entity", "unknown");

    final TelemetryDatabase.SessionRecord session;
    final ZoneId zone;
    final int dailyNumber;
    final String label;
    final List<TimelineRow> timeline;
    final List<InventorySummaryRow> inventorySummary;
    final List<InventoryChangeRow> inventoryChanges;
    final List<StorageContentRow> storageContents;
    final List<StorageChangeRow> storageChanges;
    final List<TransferRow> transfers;
    final List<EconomyRow> economy;
    final List<CombatRow> combat;
    final List<InteractionRow> interactions;
    final List<RawRow> rawRows;
    final long moneyEarned;
    final long moneySpent;
    final int attacks;
    final int defeats;
    final int deaths;
    final double lowestHealth;

    private TelemetrySessionReport(TelemetryDatabase.SessionRecord session, ZoneId zone, int dailyNumber, String username,
                                   List<TimelineRow> timeline, List<InventorySummaryRow> inventorySummary,
                                   List<InventoryChangeRow> inventoryChanges, List<StorageContentRow> storageContents,
                                   List<StorageChangeRow> storageChanges, List<TransferRow> transfers, List<EconomyRow> economy,
                                   List<CombatRow> combat, List<InteractionRow> interactions, List<RawRow> rawRows,
                                   long moneyEarned, long moneySpent, int attacks, int defeats, int deaths,
                                   double lowestHealth) {
        this.session = session;
        this.zone = zone;
        this.dailyNumber = dailyNumber;
        this.label = safeFilePart(username) + "-" + FILE_DATE.format(startedAt(session, zone)) + "-" + dailyNumber;
        this.timeline = List.copyOf(timeline);
        this.inventorySummary = List.copyOf(inventorySummary);
        this.inventoryChanges = List.copyOf(inventoryChanges);
        this.storageContents = List.copyOf(storageContents);
        this.storageChanges = List.copyOf(storageChanges);
        this.transfers = List.copyOf(transfers);
        this.economy = List.copyOf(economy);
        this.combat = List.copyOf(combat);
        this.interactions = List.copyOf(interactions);
        this.rawRows = List.copyOf(rawRows);
        this.moneyEarned = moneyEarned;
        this.moneySpent = moneySpent;
        this.attacks = attacks;
        this.defeats = defeats;
        this.deaths = deaths;
        this.lowestHealth = lowestHealth;
    }

    static TelemetrySessionReport build(TelemetryDatabase database, String sessionId, boolean discordSafe, ZoneId zone) throws Exception {
        TelemetryDatabase.SessionRecord session = database.session(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Unknown telemetry session: " + sessionId);
        }
        int dailyNumber = dailyNumber(database.sessions(), session, zone);
        List<TelemetryDatabase.StoredEvent> events = database.events(sessionId);

        List<TimelineRow> timeline = new ArrayList<>();
        List<InventoryChangeRow> inventoryChanges = new ArrayList<>();
        List<EconomyRow> economy = new ArrayList<>();
        List<CombatRow> combat = new ArrayList<>();
        List<InteractionRow> interactions = new ArrayList<>();
        List<RawRow> rawRows = new ArrayList<>();
        Map<Integer, ItemState> startingPlayerSlots = new HashMap<>();
        Map<Integer, ItemState> endingPlayerSlots = new HashMap<>();
        Map<LocationSlot, ItemState> latestLocations = new LinkedHashMap<>();
        Map<String, String> locationNames = new LinkedHashMap<>();
        Set<String> observedLocations = new LinkedHashSet<>();
        List<ItemMutation> mutations = new ArrayList<>();
        List<StorageChangeRow> storageChanges = new ArrayList<>();
        Set<String> collapsedSnapshots = new LinkedHashSet<>();
        long moneyEarned = 0L;
        long moneySpent = 0L;
        int attacks = 0;
        int defeats = 0;
        int deaths = 0;
        double lowestHealth = Double.NaN;
        String username = "player";

        for (TelemetryDatabase.StoredEvent event : events) {
            if (discordSafe && "CHAT_COMMAND".equals(event.category())) {
                continue;
            }
            ZonedDateTime time = at(event.timestampMs(), zone);
            rawRows.add(new RawRow(event.sessionId(), event.sequence(), time, event.category(), event.type(), event.dataJson()));
            JsonObject data = parseObject(event.dataJson());
            switch (event.category()) {
                case "SESSION" -> {
                    if ("session_start".equals(event.type()) && !string(data, "username").isBlank()) {
                        username = string(data, "username");
                    }
                    timeline.add(sessionTimeline(time, event.type(), data));
                }
                case "INVENTORY" -> {
                    InventoryData inventory = inventoryData(data);
                    latestLocations.put(new LocationSlot(inventory.location(), inventory.slot()), inventory.current());
                    locationNames.put(inventory.location(), inventory.locationName());
                    observedLocations.add(baseLocation(inventory.location()));
                    if ("player".equals(inventory.location())) {
                        endingPlayerSlots.put(inventory.slot(), inventory.current());
                        if ("session_start".equals(inventory.reason())) {
                            startingPlayerSlots.put(inventory.slot(), inventory.current());
                        }
                    }
                    if ("slot_snapshot".equals(event.type())) {
                        String key = inventory.reason() + "|" + inventory.location();
                        if (collapsedSnapshots.add(key)) {
                            timeline.add(new TimelineRow(time, "Inventory", snapshotActivity(inventory.reason()),
                                friendlyLocation(inventory.location())));
                        }
                    } else {
                        InventoryChangeRow row = inventoryChange(time, inventory);
                        inventoryChanges.add(row);
                        appendMutations(time, inventory, mutations, storageChanges);
                        timeline.add(new TimelineRow(time, "Inventory", row.action(), row.details()));
                    }
                }
                case "ECONOMY" -> {
                    long signed = longValue(data, "signed_amount", 0L);
                    if (signed >= 0L) moneyEarned += signed; else moneySpent += -signed;
                    EconomyRow row = new EconomyRow(time, signed >= 0L ? "Money earned" : "Money spent",
                        Math.abs(signed), friendlySource(string(data, "source")),
                        signed >= 0L ? "Received " + Math.abs(signed) + " money" : "Spent " + Math.abs(signed) + " money");
                    economy.add(row);
                    timeline.add(new TimelineRow(time, "Money", row.direction(), formatSigned(signed)));
                }
                case "COMBAT" -> {
                    CombatRow row = combatRow(time, event.type(), data);
                    combat.add(row);
                    timeline.add(new TimelineRow(time, "Combat", row.event(), combatDetails(row)));
                    if ("attack_attempt".equals(event.type())) attacks++;
                    if ("observed_target_death".equals(event.type())) defeats++;
                    if ("death".equals(event.type())) deaths++;
                    if (row.healthAfter() != null && (Double.isNaN(lowestHealth) || row.healthAfter() < lowestHealth)) {
                        lowestHealth = row.healthAfter();
                    }
                }
                case "INTERACTION" -> {
                    InteractionRow row = interactionRow(time, event.type(), data);
                    interactions.add(row);
                    timeline.add(new TimelineRow(time, "Interaction", row.action(), row.details()));
                }
                default -> {
                    // Chat and unknown technical categories remain available in Raw Data only.
                }
            }
        }

        if (!session.cleanEnd() && session.endedMs() != null
            && timeline.stream().noneMatch(row -> "Session ended".equals(row.activity()))) {
            timeline.add(new TimelineRow(at(session.endedMs(), zone), "Session", "Session interrupted",
                "Game closed unexpectedly; report recovered on next launch"));
        }

        List<InventorySummaryRow> inventorySummary = inventorySummary(startingPlayerSlots, endingPlayerSlots);
        List<StorageContentRow> storageContents = storageContents(latestLocations, locationNames, observedLocations);
        List<TransferRow> transfers = correlateTransfers(mutations);
        timeline.sort(Comparator.comparing(TimelineRow::time));
        return new TelemetrySessionReport(session, zone, dailyNumber, username, timeline, inventorySummary, inventoryChanges,
            storageContents, storageChanges, transfers,
            economy, combat, interactions, rawRows, moneyEarned, moneySpent, attacks, defeats, deaths, lowestHealth);
    }

    ZonedDateTime startedAt() {
        return startedAt(this.session, this.zone);
    }

    ZonedDateTime endedAt() {
        return this.session.endedMs() == null ? null : at(this.session.endedMs(), this.zone);
    }

    Duration duration() {
        long end = this.session.endedMs() == null ? this.session.startedMs() : this.session.endedMs();
        return Duration.ofMillis(Math.max(0L, end - this.session.startedMs()));
    }

    String durationText() {
        long seconds = duration().toSeconds();
        long hours = seconds / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        long remaining = seconds % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m " + remaining + "s";
        if (minutes > 0L) return minutes + "m " + remaining + "s";
        return remaining + "s";
    }

    long netMoney() {
        return this.moneyEarned - this.moneySpent;
    }

    int itemsGained() {
        return this.inventorySummary.stream().mapToInt(row -> Math.max(0, row.change())).sum();
    }

    int itemsLost() {
        return this.inventorySummary.stream().mapToInt(row -> Math.max(0, -row.change())).sum();
    }

    int shulkersInspected() {
        return (int) this.storageContents.stream().filter(row -> row.depth() == 0 && row.shulker()).count();
    }

    int petsStored() {
        return this.storageContents.stream().filter(row -> row.depth() == 0 && row.location().startsWith("Pet Storage")
            && row.quantity() > 0).mapToInt(StorageContentRow::quantity).sum();
    }

    int fruitsStored() {
        return this.storageContents.stream().filter(row -> row.depth() == 0 && row.location().startsWith("Fruit Storage")
            && row.quantity() > 0).mapToInt(StorageContentRow::quantity).sum();
    }

    List<InventorySummaryRow> topGained(int limit) {
        return this.inventorySummary.stream().filter(row -> row.change() > 0)
            .sorted(Comparator.comparingInt(InventorySummaryRow::change).reversed()).limit(limit).toList();
    }

    List<InventorySummaryRow> topLost(int limit) {
        return this.inventorySummary.stream().filter(row -> row.change() < 0)
            .sorted(Comparator.comparingInt(InventorySummaryRow::change)).limit(limit).toList();
    }

    List<TimelineSummaryRow> timelineSummary() {
        Map<MinuteCategory, TimelineAccumulator> groups = new LinkedHashMap<>();
        for (TimelineRow row : this.timeline) {
            if ("Combat".equals(row.category()) && "Health changed".equals(row.activity())) continue;
            ZonedDateTime minute = row.time().withSecond(0).withNano(0);
            TimelineAccumulator group = groups.computeIfAbsent(new MinuteCategory(minute, row.category()),
                ignored -> new TimelineAccumulator());
            group.activities.merge(row.activity(), 1, Integer::sum);
            if (!row.details().isBlank()) group.details.merge(row.details(), 1, Integer::sum);
            group.events++;
        }
        return groups.entrySet().stream()
            .sorted(Map.Entry.<MinuteCategory, TimelineAccumulator>comparingByKey(
                Comparator.comparing(MinuteCategory::minute).thenComparingInt(key -> categoryOrder(key.category()))))
            .map(entry -> new TimelineSummaryRow(entry.getKey().minute(), entry.getKey().category(),
                compactCounts(entry.getValue().activities, 4), compactCounts(entry.getValue().details, 3),
                entry.getValue().events))
            .toList();
    }

    List<StorageLocationSummaryRow> storageLocationSummary() {
        Map<String, StorageLocationAccumulator> groups = new LinkedHashMap<>();
        for (StorageContentRow row : this.storageContents) {
            StorageLocationAccumulator group = groups.computeIfAbsent(row.location(),
                ignored -> new StorageLocationAccumulator());
            if (row.quantity() > 0) {
                group.items.add(row.item());
                group.quantity += row.quantity();
                group.hasContents = true;
            }
            if (row.depth() == 0 && row.shulker()) group.shulkers++;
            if (row.item().contains("Not inspected") || row.status().contains("Open this storage")) {
                group.notInspected = true;
            }
        }
        return groups.entrySet().stream().sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .map(entry -> {
                StorageLocationAccumulator value = entry.getValue();
                String status = value.hasContents ? "Latest known" : value.notInspected ? "Not inspected" : "Empty";
                return new StorageLocationSummaryRow(entry.getKey(), value.items.size(), value.quantity,
                    value.shulkers, status);
            }).toList();
    }

    List<StorageItemSummaryRow> storageItemSummary() {
        Map<StorageItemKey, Integer> groups = new LinkedHashMap<>();
        for (StorageContentRow row : this.storageContents) {
            if (row.quantity() <= 0) continue;
            String container = row.depth() > 0 && row.status().startsWith("Inside ")
                ? row.status().substring("Inside ".length()) : "";
            groups.merge(new StorageItemKey(row.location(), container, row.item()), row.quantity(), Integer::sum);
        }
        return groups.entrySet().stream()
            .sorted(Map.Entry.<StorageItemKey, Integer>comparingByKey(Comparator
                .comparing(StorageItemKey::location, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StorageItemKey::container, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StorageItemKey::item, String.CASE_INSENSITIVE_ORDER)))
            .map(entry -> new StorageItemSummaryRow(entry.getKey().location(), entry.getKey().container(),
                entry.getKey().item(), entry.getValue()))
            .toList();
    }

    List<StorageChangeSummaryRow> storageChangeSummary() {
        Map<StorageChangeKey, int[]> groups = new LinkedHashMap<>();
        for (StorageChangeRow row : this.storageChanges) {
            int[] totals = groups.computeIfAbsent(new StorageChangeKey(row.location(), row.item()), ignored -> new int[2]);
            if ("Added".equals(row.action())) totals[0] += row.quantity();
            if ("Removed".equals(row.action())) totals[1] += row.quantity();
        }
        return groups.entrySet().stream()
            .sorted(Map.Entry.<StorageChangeKey, int[]>comparingByKey(Comparator
                .comparing(StorageChangeKey::location, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StorageChangeKey::item, String.CASE_INSENSITIVE_ORDER)))
            .map(entry -> new StorageChangeSummaryRow(entry.getKey().location(), entry.getKey().item(),
                entry.getValue()[0], entry.getValue()[1], entry.getValue()[0] - entry.getValue()[1]))
            .toList();
    }

    List<TransferSummaryRow> transferSummary() {
        Map<String, long[]> groups = new LinkedHashMap<>();
        for (TransferRow row : this.transfers) {
            long[] totals = groups.computeIfAbsent(row.item(), ignored -> new long[4]);
            switch (row.result()) {
                case "Received" -> totals[0] += row.quantity();
                case "Moved" -> totals[1] += row.quantity();
                case "Sent", "Listed" -> totals[2] += row.quantity();
                case "Discarded", "Dropped" -> totals[3] += row.quantity();
                default -> {
                    // Unknown outcomes remain available in Raw Data.
                }
            }
        }
        return groups.entrySet().stream().sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .map(entry -> new TransferSummaryRow(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                entry.getValue()[2], entry.getValue()[3],
                entry.getValue()[0] - entry.getValue()[2] - entry.getValue()[3]))
            .toList();
    }

    List<EconomySummaryRow> economySummary() {
        Map<EconomySummaryKey, EconomyAccumulator> groups = new LinkedHashMap<>();
        for (EconomyRow row : this.economy) {
            EconomyAccumulator group = groups.computeIfAbsent(new EconomySummaryKey(row.direction(), row.source()),
                ignored -> new EconomyAccumulator());
            group.count++;
            group.total += row.amount();
            group.minimum = Math.min(group.minimum, row.amount());
            group.maximum = Math.max(group.maximum, row.amount());
        }
        return groups.entrySet().stream()
            .sorted(Comparator.<Map.Entry<EconomySummaryKey, EconomyAccumulator>>
                comparingInt(entry -> economyOrder(entry.getKey().direction()))
                .thenComparing((left, right) -> Long.compare(right.getValue().total, left.getValue().total))
                .thenComparing(entry -> entry.getKey().source(), String.CASE_INSENSITIVE_ORDER))
            .map(entry -> new EconomySummaryRow(entry.getKey().direction(), entry.getKey().source(),
                entry.getValue().count, entry.getValue().total,
                entry.getValue().count == 0 ? 0D : (double) entry.getValue().total / entry.getValue().count,
                entry.getValue().minimum == Long.MAX_VALUE ? 0L : entry.getValue().minimum,
                entry.getValue().maximum == Long.MIN_VALUE ? 0L : entry.getValue().maximum))
            .toList();
    }

    List<CombatSummaryRow> combatSummary() {
        Map<CombatSummaryKey, Integer> groups = new LinkedHashMap<>();
        for (CombatRow row : this.combat) {
            if ("Health changed".equals(row.event())) continue;
            groups.merge(new CombatSummaryKey(row.event(), row.target()), 1, Integer::sum);
        }
        return groups.entrySet().stream()
            .sorted(Comparator.<Map.Entry<CombatSummaryKey, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(entry -> entry.getKey().event(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.getKey().target(), String.CASE_INSENSITIVE_ORDER))
            .map(entry -> new CombatSummaryRow(entry.getKey().event(), entry.getKey().target(), entry.getValue()))
            .toList();
    }

    List<InteractionSummaryRow> interactionSummary() {
        Map<InteractionSummaryKey, Integer> groups = new LinkedHashMap<>();
        for (InteractionRow row : this.interactions) {
            String target = row.target().startsWith("Inventory slot ") ? "Inventory slots" : row.target();
            groups.merge(new InteractionSummaryKey(row.action(), target, row.hand()), 1, Integer::sum);
        }
        return groups.entrySet().stream()
            .sorted(Comparator.<Map.Entry<InteractionSummaryKey, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(entry -> entry.getKey().action(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.getKey().target(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.getKey().context(), String.CASE_INSENSITIVE_ORDER))
            .map(entry -> new InteractionSummaryRow(entry.getKey().action(), entry.getKey().target(),
                entry.getKey().context(), entry.getValue()))
            .toList();
    }

    long transferQuantity(String result) {
        return this.transfers.stream().filter(row -> result.equals(row.result())).mapToLong(TransferRow::quantity).sum();
    }

    private static int categoryOrder(String category) {
        return switch (category) {
            case "Session" -> 0;
            case "Money" -> 1;
            case "Combat" -> 2;
            case "Inventory" -> 3;
            case "Interaction" -> 4;
            default -> 5;
        };
    }

    private static int economyOrder(String direction) {
        return "Money earned".equals(direction) ? 0 : "Money spent".equals(direction) ? 1 : 2;
    }

    private static String compactCounts(Map<String, Integer> counts, int limit) {
        if (counts.isEmpty()) return "";
        List<Map.Entry<String, Integer>> ordered = counts.entrySet().stream()
            .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
            .toList();
        List<String> parts = new ArrayList<>();
        for (int index = 0; index < Math.min(limit, ordered.size()); index++) {
            Map.Entry<String, Integer> entry = ordered.get(index);
            parts.add(entry.getKey() + (entry.getValue() > 1 ? " x" + entry.getValue() : ""));
        }
        if (ordered.size() > limit) parts.add("+" + (ordered.size() - limit) + " more");
        return String.join("; ", parts);
    }

    private static String safeFilePart(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_")
            .replaceAll("[. ]+$", "").trim();
        return cleaned.isBlank() ? "player" : cleaned;
    }

    private static int dailyNumber(List<TelemetryDatabase.SessionRecord> sessions,
                                   TelemetryDatabase.SessionRecord target, ZoneId zone) {
        LocalDate date = startedAt(target, zone).toLocalDate();
        int index = 0;
        for (TelemetryDatabase.SessionRecord session : sessions.stream()
            .sorted(Comparator.comparingLong(TelemetryDatabase.SessionRecord::startedMs)
                .thenComparing(TelemetryDatabase.SessionRecord::id)).toList()) {
            if (startedAt(session, zone).toLocalDate().equals(date)) {
                index++;
                if (session.id().equals(target.id())) return index;
            }
        }
        return Math.max(1, index + 1);
    }

    private static List<InventorySummaryRow> inventorySummary(Map<Integer, ItemState> startSlots,
                                                               Map<Integer, ItemState> endSlots) {
        Map<ItemKey, Integer> start = aggregate(startSlots);
        Map<ItemKey, Integer> end = aggregate(endSlots);
        Set<ItemKey> keys = new LinkedHashSet<>();
        keys.addAll(start.keySet());
        keys.addAll(end.keySet());
        return keys.stream().map(key -> new InventorySummaryRow(key.name(), start.getOrDefault(key, 0),
                end.getOrDefault(key, 0), end.getOrDefault(key, 0) - start.getOrDefault(key, 0)))
            .filter(row -> row.starting() != 0 || row.ending() != 0)
            .sorted(Comparator.comparing(InventorySummaryRow::item, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private static Map<ItemKey, Integer> aggregate(Map<Integer, ItemState> slots) {
        Map<ItemKey, Integer> totals = new LinkedHashMap<>();
        for (ItemState state : slots.values()) {
            if (state.count() <= 0 || "minecraft:air".equals(state.id())) continue;
            totals.merge(new ItemKey(state.id(), state.name()), state.count(), Integer::sum);
        }
        return totals;
    }

    private static List<StorageContentRow> storageContents(Map<LocationSlot, ItemState> latest,
                                                            Map<String, String> locationNames,
                                                            Set<String> observedLocations) {
        List<Map.Entry<LocationSlot, ItemState>> entries = latest.entrySet().stream()
            .filter(entry -> entry.getValue().count() > 0 && !"minecraft:air".equals(entry.getValue().id()))
            .sorted(Comparator.comparingInt((Map.Entry<LocationSlot, ItemState> entry) -> locationOrder(entry.getKey().location()))
                .thenComparing(entry -> entry.getKey().location()).thenComparingInt(entry -> entry.getKey().slot())).toList();
        List<StorageContentRow> rows = new ArrayList<>();
        Map<String, Integer> menuNumbers = new HashMap<>();
        int shulkerNumber = 0;
        for (Map.Entry<LocationSlot, ItemState> entry : entries) {
            LocationSlot key = entry.getKey();
            ItemState state = entry.getValue();
            String location = friendlyLocation(key.location(), locationNames.get(key.location()));
            String slot = friendlySlot(key.location(), key.slot());
            if (baseLocation(key.location()).equals("pet_storage")) {
                slot = "Pet #" + menuNumbers.merge("pet_storage", 1, Integer::sum);
            } else if (baseLocation(key.location()).equals("fruit_storage")) {
                slot = "Fruit #" + menuNumbers.merge("fruit_storage", 1, Integer::sum);
            }
            boolean shulker = isShulker(state);
            String item = state.name();
            if (shulker) item = shulkerLabel(state, ++shulkerNumber);
            rows.add(new StorageContentRow(location, slot, 0, item, state.count(), "Latest known", shulker));
            if (shulker) {
                if (!state.containerKnown()) {
                    rows.add(new StorageContentRow(location, "↳ Shulker contents", 1,
                        "Contents not supplied by server", 0, "Unavailable", false));
                } else if (state.contents().isEmpty()) {
                    rows.add(new StorageContentRow(location, "↳ Shulker contents", 1,
                        "Empty", 0, "Shulker is empty", false));
                } else {
                    for (ContainedState content : state.contents()) {
                        if (content.item().count() <= 0) continue;
                        rows.add(new StorageContentRow(location, "↳ Shulker slot " + (content.slot() + 1), 1,
                            content.item().name(), content.item().count(), "Inside " + item, isShulker(content.item())));
                    }
                }
            }
        }
        addStorageStatus(rows, observedLocations, "pet_storage", "Pet Storage");
        addStorageStatus(rows, observedLocations, "fruit_storage", "Fruit Storage");
        return rows;
    }

    private static void addStorageStatus(List<StorageContentRow> rows, Set<String> observedLocations,
                                         String rawLocation, String displayName) {
        if (rows.stream().anyMatch(row -> row.location().startsWith(displayName))) return;
        boolean observed = observedLocations.contains(rawLocation);
        rows.add(new StorageContentRow(displayName, "", 0, observed ? "Empty" : "Not inspected during this session",
            0, observed ? "No stored entries were visible" : "Open this storage to record it", false));
    }

    private static void appendMutations(ZonedDateTime time, InventoryData inventory,
                                        List<ItemMutation> mutations, List<StorageChangeRow> storageChanges) {
        String location = friendlyLocation(inventory.location(), inventory.locationName());
        String slot = friendlySlot(inventory.location(), inventory.slot());
        appendStateDifference(time, inventory.previous(), inventory.current(), location, slot,
            inventory.contextInventory(), inventory.contextName(), inventory.interactionAction(), mutations, storageChanges);

        if (isShulker(inventory.previous()) || isShulker(inventory.current())) {
            String shulkerName = inventory.current().count() > 0 ? inventory.current().name() : inventory.previous().name();
            Map<Integer, ItemState> before = contentsBySlot(inventory.previous().contents());
            Map<Integer, ItemState> after = contentsBySlot(inventory.current().contents());
            Set<Integer> slots = new LinkedHashSet<>();
            slots.addAll(before.keySet());
            slots.addAll(after.keySet());
            for (int innerSlot : slots) {
                appendStateDifference(time, before.getOrDefault(innerSlot, ItemState.EMPTY),
                    after.getOrDefault(innerSlot, ItemState.EMPTY), location + " / " + shulkerName,
                    "Shulker slot " + (innerSlot + 1), inventory.contextInventory(), inventory.contextName(),
                    inventory.interactionAction(), mutations, storageChanges);
            }
        }
    }

    private static Map<Integer, ItemState> contentsBySlot(List<ContainedState> contents) {
        Map<Integer, ItemState> result = new LinkedHashMap<>();
        for (ContainedState state : contents) result.put(state.slot(), state.item());
        return result;
    }

    private static void appendStateDifference(ZonedDateTime time, ItemState before, ItemState after,
                                              String location, String slot, String contextInventory,
                                              String contextName, String interactionAction,
                                              List<ItemMutation> mutations, List<StorageChangeRow> changes) {
        boolean beforePresent = before.count() > 0 && !"minecraft:air".equals(before.id());
        boolean afterPresent = after.count() > 0 && !"minecraft:air".equals(after.id());
        if (beforePresent && afterPresent && sameItem(before, after)) {
            int delta = after.count() - before.count();
            if (delta < 0) addMutation(time, before, -delta, false, location, slot, contextInventory, contextName,
                interactionAction, mutations, changes);
            if (delta > 0) addMutation(time, after, delta, true, location, slot, contextInventory, contextName,
                interactionAction, mutations, changes);
            return;
        }
        if (beforePresent) addMutation(time, before, before.count(), false, location, slot, contextInventory,
            contextName, interactionAction, mutations, changes);
        if (afterPresent) addMutation(time, after, after.count(), true, location, slot, contextInventory,
            contextName, interactionAction, mutations, changes);
    }

    private static void addMutation(ZonedDateTime time, ItemState item, int quantity, boolean addition,
                                    String location, String slot, String contextInventory, String contextName,
                                    String interactionAction, List<ItemMutation> mutations,
                                    List<StorageChangeRow> changes) {
        ItemMutation mutation = new ItemMutation(time, item, quantity, addition, location, slot,
            contextInventory, contextName, interactionAction);
        mutations.add(mutation);
        changes.add(new StorageChangeRow(time, location, slot, addition ? "Added" : "Removed",
            item.name(), quantity, (addition ? "Added to " : "Removed from ") + location));
    }

    private static List<TransferRow> correlateTransfers(List<ItemMutation> mutations) {
        int[] remaining = mutations.stream().mapToInt(ItemMutation::quantity).toArray();
        List<TransferRow> rows = new ArrayList<>();
        for (int i = 0; i < mutations.size(); i++) {
            ItemMutation removal = mutations.get(i);
            if (removal.addition()) continue;
            for (int j = 0; j < mutations.size() && remaining[i] > 0; j++) {
                ItemMutation addition = mutations.get(j);
                if (!addition.addition() || remaining[j] <= 0 || !sameItem(removal.item(), addition.item())) continue;
                long millis = Math.abs(Duration.between(removal.time(), addition.time()).toMillis());
                if (millis > 2_500L) continue;
                int quantity = Math.min(remaining[i], remaining[j]);
                rows.add(new TransferRow(removal.time(), removal.item().name(), quantity,
                    locationSlot(removal), locationSlot(addition), "Moved"));
                remaining[i] -= quantity;
                remaining[j] -= quantity;
            }
        }
        for (int i = 0; i < mutations.size(); i++) {
            if (remaining[i] <= 0) continue;
            ItemMutation mutation = mutations.get(i);
            if (mutation.addition()) {
                String from = contextLocation(mutation.contextInventory(), mutation.contextName(), "Unknown source");
                rows.add(new TransferRow(mutation.time(), mutation.item().name(), remaining[i], from,
                    locationSlot(mutation), "Received"));
            } else {
                String to;
                String result;
                if ("THROW".equals(mutation.interactionAction())) {
                    to = "Ground (dropped)";
                    result = "Dropped";
                } else {
                    to = contextLocation(mutation.contextInventory(), mutation.contextName(), "Unknown destination");
                    result = to.equals("Trash") ? "Discarded" : to.equals("Auction House") ? "Listed" : "Sent";
                }
                rows.add(new TransferRow(mutation.time(), mutation.item().name(), remaining[i],
                    locationSlot(mutation), to, result));
            }
        }
        rows.sort(Comparator.comparing(TransferRow::time));
        return rows;
    }

    private static String locationSlot(ItemMutation mutation) {
        return mutation.location() + (mutation.slot().isBlank() ? "" : " — " + mutation.slot());
    }

    private static String contextLocation(String raw, String name, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return friendlyLocation(raw, name);
    }

    private static boolean sameItem(ItemState left, ItemState right) {
        return left.id().equals(right.id()) && left.name().equals(right.name());
    }

    private static boolean isShulker(ItemState state) {
        return state.id().endsWith("shulker_box");
    }

    private static String shulkerLabel(ItemState state, int number) {
        String generic = friendlyIdentifier(state.id());
        String name = state.name();
        if (name.isBlank() || name.equalsIgnoreCase("Shulker Box") || name.equalsIgnoreCase(generic)) {
            return generic + " #" + number;
        }
        return name + " (Shulker #" + number + ")";
    }

    private static int locationOrder(String raw) {
        String base = baseLocation(raw);
        if (base.equals("player")) return 0;
        if (base.equals("ender_chest")) return 10;
        if (base.startsWith("island_chest_")) return 20;
        if (base.equals("pet_storage")) return 30;
        if (base.equals("fruit_storage")) return 40;
        if (base.startsWith("opened_shulker:")) return 50;
        return 60;
    }

    private static String baseLocation(String raw) {
        if (raw == null) return "";
        return raw.replaceFirst("_page_[0-9]+$", "");
    }

    private static TimelineRow sessionTimeline(ZonedDateTime time, String type, JsonObject data) {
        return switch (type) {
            case "session_start" -> new TimelineRow(time, "Session", "Session started", string(data, "server"));
            case "session_end" -> new TimelineRow(time, "Session", "Session ended", "Telemetry recording completed");
            default -> new TimelineRow(time, "Session", friendlyIdentifier(type), "");
        };
    }

    private static InventoryData inventoryData(JsonObject data) {
        String location = string(data, "inventory");
        String reason = string(data, "reason");
        int slot = intValue(data, "slot", -1);
        ItemState current = itemState(data, "", "contents");
        ItemState previous = itemState(data, "previous_", "previous_contents");
        return new InventoryData(location, string(data, "inventory_name"), reason, slot, previous, current,
            string(data, "context_inventory"), string(data, "context_name"), string(data, "interaction_action"));
    }

    private static ItemState itemState(JsonObject data, String prefix, String contentsKey) {
        String id = string(data, prefix + "item_id");
        String name = friendlyItem(string(data, prefix + "name"), id);
        int count = intValue(data, prefix + "count", 0);
        String signature = string(data, prefix + "signature");
        boolean containerKnown = booleanValue(data, prefix + "container_contents_known", false);
        List<ContainedState> contents = new ArrayList<>();
        JsonElement element = data.get(contentsKey);
        if (element != null && element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (!child.isJsonObject()) continue;
                JsonObject object = child.getAsJsonObject();
                contents.add(new ContainedState(intValue(object, "slot", -1), itemState(object, "", "contents")));
            }
            if (!contents.isEmpty()) containerKnown = true;
        }
        return new ItemState(id, name, count, signature, containerKnown, List.copyOf(contents));
    }

    private static InventoryChangeRow inventoryChange(ZonedDateTime time, InventoryData data) {
        int change = data.current().count() - data.previous().count();
        String action;
        String details;
        if (!data.previous().id().isBlank() && !data.previous().id().equals(data.current().id())
            && data.previous().count() > 0 && data.current().count() > 0) {
            action = "Item replaced";
            details = data.previous().name() + " replaced with " + data.current().name();
        } else if (data.current().count() <= 0) {
            action = "Item removed";
            details = data.previous().name();
        } else if (data.previous().count() <= 0) {
            action = "Item added";
            details = data.current().name();
        } else if (change > 0) {
            action = "Quantity increased";
            details = data.current().name() + " (" + formatSigned(change) + ")";
        } else if (change < 0) {
            action = "Quantity decreased";
            details = data.current().name() + " (" + formatSigned(change) + ")";
        } else {
            action = "Item changed";
            details = data.current().name();
        }
        String item = data.current().count() > 0 ? data.current().name() : data.previous().name();
        return new InventoryChangeRow(time, friendlyLocation(data.location(), data.locationName()), action, item,
            data.previous().count(), data.current().count(), change, friendlySlot(data.location(), data.slot()), details);
    }

    private static CombatRow combatRow(ZonedDateTime time, String type, JsonObject data) {
        return switch (type) {
            case "vitals_change" -> {
                double before = doubleValue(data, "previous_health", 0D);
                double after = doubleValue(data, "health", 0D);
                yield new CombatRow(time, "Health changed", "", before, after, after - before);
            }
            case "attack_attempt" -> new CombatRow(time, "Mob attacked",
                friendlyEntity(string(data, "target_name"), string(data, "target_type")), null, null, null);
            case "observed_target_death" -> new CombatRow(time, "Mob defeated",
                friendlyEntity(string(data, "target_name"), string(data, "target_type")), null, null, null);
            case "death" -> new CombatRow(time, "Player died", "", null, null, null);
            case "respawn" -> new CombatRow(time, "Player respawned", "", null, null, null);
            default -> new CombatRow(time, friendlyIdentifier(type), "", null, null, null);
        };
    }

    private static InteractionRow interactionRow(ZonedDateTime time, String type, JsonObject data) {
        return switch (type) {
            case "use_block" -> new InteractionRow(time, "Used block", friendlyIdentifier(string(data, "block")),
                friendlyHand(string(data, "hand")), null, "Used " + friendlyIdentifier(string(data, "block")));
            case "attack_block" -> new InteractionRow(time, "Hit block", friendlyIdentifier(string(data, "block")),
                "", null, "Hit " + friendlyIdentifier(string(data, "block")));
            case "use_entity" -> {
                String target = friendlyEntity(string(data, "entity_name"), string(data, "entity_type"));
                yield new InteractionRow(time, "Interacted with mob", target, friendlyHand(string(data, "hand")),
                    null, target);
            }
            case "use_item" -> {
                String item = friendlyItem(string(data, "item_name"), string(data, "item"));
                yield new InteractionRow(time, "Used item", item, friendlyHand(string(data, "hand")), null, item);
            }
            case "inventory_click" -> {
                int slot = intValue(data, "slot", -1);
                String action = friendlyInventoryAction(string(data, "action"));
                yield new InteractionRow(time, action, "Inventory slot " + slot, "", slot, "Inventory slot " + slot);
            }
            default -> new InteractionRow(time, friendlyIdentifier(type), "", "", null, "");
        };
    }

    private static String combatDetails(CombatRow row) {
        if (!row.target().isBlank()) return row.target();
        if (row.healthBefore() != null && row.healthAfter() != null) {
            return oneDecimal(row.healthBefore()) + " to " + oneDecimal(row.healthAfter());
        }
        return "";
    }

    static String friendlyItem(String displayName, String identifier) {
        String name = cleanName(displayName);
        if (!name.isBlank() && !"air".equalsIgnoreCase(name)) return name;
        String fallback = friendlyIdentifier(identifier);
        return fallback.isBlank() || "Air".equals(fallback) ? "Empty slot" : fallback;
    }

    static String friendlyEntity(String displayName, String identifier) {
        String id = stripNamespace(identifier).toLowerCase(Locale.ROOT);
        String name = cleanName(displayName);
        if ("interaction".equals(id)
            || (!name.isBlank() && GENERIC_ENTITY_NAMES.contains(name.toLowerCase(Locale.ROOT)))) {
            return "Mob";
        }
        if ("player".equals(id)) return name.isBlank() ? "Player" : "Player — " + name;
        if (name.isBlank()) name = friendlyIdentifier(identifier);
        return name.isBlank() ? "Mob" : "Mob — " + name;
    }

    static String friendlyIdentifier(String raw) {
        String value = stripNamespace(raw);
        if (value.isBlank()) return "";
        value = value.replace('/', ' ').replace('_', ' ').replace('-', ' ').trim();
        StringBuilder result = new StringBuilder();
        for (String word : value.split("\\s+")) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) result.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    private static String friendlyLocation(String raw) {
        return friendlyLocation(raw, "");
    }

    private static String friendlyLocation(String raw, String suppliedName) {
        if (raw == null || raw.isBlank() || "player".equals(raw)) return "Player inventory";
        if ("ender_chest".equals(raw)) return "Ender Chest";
        if (raw.startsWith("island_chest_")) return "Island Chest " + raw.substring("island_chest_".length());
        if (baseLocation(raw).equals("pet_storage")) return "Pet Storage" + pageSuffix(raw);
        if (baseLocation(raw).equals("fruit_storage")) return "Fruit Storage" + pageSuffix(raw);
        if ("trash".equals(raw)) return "Trash";
        if ("auction_house".equals(raw)) return "Auction House";
        if (raw.startsWith("opened_shulker:")) return suppliedName == null || suppliedName.isBlank() ? "Opened Shulker" : suppliedName;
        if (raw.startsWith("opened_container:")) return suppliedName == null || suppliedName.isBlank() ? "Opened Container" : suppliedName;
        if ("opened_container".equals(raw)) return "Opened Container";
        return suppliedName == null || suppliedName.isBlank() ? friendlyIdentifier(raw) : suppliedName;
    }

    private static String pageSuffix(String raw) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("_page_([0-9]+)$").matcher(raw);
        return matcher.find() ? " — Page " + matcher.group(1) : "";
    }

    private static String friendlySlot(String location, int slot) {
        if ("player".equals(location)) {
            if (slot >= 0 && slot <= 8) return "Hotbar slot " + (slot + 1);
            if (slot >= 9 && slot <= 35) return "Inventory slot " + (slot - 8);
            return switch (slot) {
                case 36 -> "Armor — Boots";
                case 37 -> "Armor — Leggings";
                case 38 -> "Armor — Chestplate";
                case 39 -> "Armor — Helmet";
                case 40 -> "Off-hand";
                default -> "Slot " + (slot + 1);
            };
        }
        if (slot < 0) return "";
        return "Slot " + (slot + 1);
    }

    private static String friendlyHand(String raw) {
        return switch (raw == null ? "" : raw) {
            case "MAIN_HAND" -> "Main hand";
            case "OFF_HAND" -> "Off hand";
            default -> friendlyIdentifier(raw);
        };
    }

    private static String friendlyInventoryAction(String raw) {
        return switch (raw == null ? "" : raw) {
            case "PICKUP" -> "Picked up item";
            case "QUICK_MOVE" -> "Quick-moved item";
            case "PICKUP_ALL" -> "Picked up matching items";
            case "THROW" -> "Dropped item";
            case "SWAP" -> "Swapped items";
            case "CLONE" -> "Copied item";
            case "QUICK_CRAFT" -> "Quick-crafted items";
            default -> friendlyIdentifier(raw);
        };
    }

    private static String friendlySource(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase(Locale.ROOT)) {
            case "actionbar" -> "On-screen notification";
            case "chat" -> "Game message";
            default -> friendlyIdentifier(raw);
        };
    }

    private static String snapshotActivity(String reason) {
        return switch (reason) {
            case "session_start" -> "Starting inventory recorded";
            case "session_end" -> "Ending inventory recorded";
            case "container_open" -> "Container contents recorded";
            default -> "Inventory snapshot recorded";
        };
    }

    private static String cleanName(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("§.", "").replaceAll("\\s+", " ").trim();
    }

    private static String stripNamespace(String raw) {
        if (raw == null) return "";
        int colon = raw.indexOf(':');
        return (colon >= 0 ? raw.substring(colon + 1) : raw).trim();
    }

    private static JsonObject parseObject(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException ignored) {
            return new JsonObject();
        }
    }

    private static String string(JsonObject object, String key) {
        try {
            JsonElement value = object.get(key);
            return value == null || value.isJsonNull() ? "" : value.getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        try {
            return object.has(key) ? object.get(key).getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double doubleValue(JsonObject object, String key, double fallback) {
        try {
            return object.has(key) ? object.get(key).getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
        try {
            return object.has(key) ? object.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static ZonedDateTime startedAt(TelemetryDatabase.SessionRecord session, ZoneId zone) {
        return at(session.startedMs(), zone);
    }

    private static ZonedDateTime at(long epochMs, ZoneId zone) {
        return Instant.ofEpochMilli(epochMs).atZone(zone);
    }

    private static String formatSigned(long value) {
        return (value > 0 ? "+" : "") + value;
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    record TimelineRow(ZonedDateTime time, String category, String activity, String details) {
    }

    record TimelineSummaryRow(ZonedDateTime minute, String category, String summary, String highlights, int events) {
    }

    record InventorySummaryRow(String item, int starting, int ending, int change) {
    }

    record InventoryChangeRow(ZonedDateTime time, String location, String action, String item, int previousQuantity,
                              int newQuantity, int change, String slot, String details) {
    }

    record StorageContentRow(String location, String slot, int depth, String item, int quantity, String status,
                             boolean shulker) {
    }

    record StorageChangeRow(ZonedDateTime time, String location, String slot, String action, String item,
                            int quantity, String details) {
    }

    record StorageLocationSummaryRow(String location, int distinctItems, int totalQuantity, int shulkers, String status) {
    }

    record StorageItemSummaryRow(String location, String container, String item, int quantity) {
    }

    record StorageChangeSummaryRow(String location, String item, int added, int removed, int net) {
    }

    record TransferRow(ZonedDateTime time, String item, int quantity, String from, String to, String result) {
    }

    record TransferSummaryRow(String item, long received, long moved, long sentOrListed, long discardedOrDropped,
                              long net) {
    }

    record EconomyRow(ZonedDateTime time, String direction, long amount, String source, String description) {
    }

    record EconomySummaryRow(String direction, String source, int transactions, long total, double average,
                             long minimum, long maximum) {
    }

    record CombatRow(ZonedDateTime time, String event, String target, Double healthBefore, Double healthAfter, Double change) {
    }

    record CombatSummaryRow(String event, String target, int count) {
    }

    record InteractionRow(ZonedDateTime time, String action, String target, String hand, Integer slot, String details) {
    }

    record InteractionSummaryRow(String action, String target, String context, int count) {
    }

    record RawRow(String sessionId, long sequence, ZonedDateTime time, String category, String type, String json) {
    }

    private record ItemKey(String id, String name) {
    }

    private record MinuteCategory(ZonedDateTime minute, String category) {
    }

    private record StorageItemKey(String location, String container, String item) {
    }

    private record StorageChangeKey(String location, String item) {
    }

    private record EconomySummaryKey(String direction, String source) {
    }

    private record CombatSummaryKey(String event, String target) {
    }

    private record InteractionSummaryKey(String action, String target, String context) {
    }

    private static final class TimelineAccumulator {
        private final Map<String, Integer> activities = new LinkedHashMap<>();
        private final Map<String, Integer> details = new LinkedHashMap<>();
        private int events;
    }

    private static final class StorageLocationAccumulator {
        private final Set<String> items = new LinkedHashSet<>();
        private int quantity;
        private int shulkers;
        private boolean hasContents;
        private boolean notInspected;
    }

    private static final class EconomyAccumulator {
        private int count;
        private long total;
        private long minimum = Long.MAX_VALUE;
        private long maximum = Long.MIN_VALUE;
    }

    private record ItemState(String id, String name, int count, String signature, boolean containerKnown,
                             List<ContainedState> contents) {
        private static final ItemState EMPTY = new ItemState("minecraft:air", "Empty slot", 0, "", false, List.of());
    }

    private record ContainedState(int slot, ItemState item) {
    }

    private record InventoryData(String location, String locationName, String reason, int slot, ItemState previous,
                                 ItemState current, String contextInventory, String contextName,
                                 String interactionAction) {
    }

    private record LocationSlot(String location, int slot) {
    }

    private record ItemMutation(ZonedDateTime time, ItemState item, int quantity, boolean addition, String location,
                                String slot, String contextInventory, String contextName, String interactionAction) {
    }
}
