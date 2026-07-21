package net.minepiece.qol.telemetry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;

public final class TelemetryManager {
    private static final Gson GSON = new Gson();
    private static final int MAX_QUEUE = 20_000;
    private static final long[] RETRY_DELAYS_MS = {5_000L, 30_000L, 120_000L, 600_000L};

    private final TelemetryConfigManager configManager;
    private final TelemetryConfig config;
    private final LinkedBlockingQueue<Work> queue = new LinkedBlockingQueue<>(MAX_QUEUE);
    private final AtomicLong droppedEvents = new AtomicLong();
    private final Thread worker;
    private final DiscordWebhookClient discord = new DiscordWebhookClient();

    private volatile boolean running = true;
    private volatile String storageStatus = "starting";
    private volatile int pendingDiscord;
    private String sessionId;
    private String serverAddress = "";
    private long sequence;
    private long sessionStartedMs;
    private int ticks;
    private float lastHealth = Float.NaN;
    private float lastAbsorption = Float.NaN;
    private boolean wasDead;
    private Entity lastAttackedEntity;
    private Map<Integer, ItemSnapshot> playerInventory = Map.of();
    private Object currentContainerScreen;
    private String pendingContainer = "";
    private String activeContainer = "";
    private String activeContainerName = "";
    private Map<Integer, ItemSnapshot> containerInventory = Map.of();
    private final Map<String, Map<Integer, ItemSnapshot>> knownContainers = new LinkedHashMap<>();
    private SlotActionType lastSlotAction;
    private long lastSlotActionMs;

    public TelemetryManager(Path modConfigDir) {
        this.configManager = new TelemetryConfigManager(modConfigDir);
        this.config = this.configManager.load();
        this.worker = new Thread(this::workerLoop, "minepiece-qol-telemetry");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public TelemetryConfig config() {
        return this.config;
    }

    public void saveConfig() {
        this.configManager.save(this.config);
    }

    public void setEnabled(boolean enabled, MinecraftClient client) {
        this.config.enabled = enabled;
        saveConfig();
        if (!enabled) {
            finishSession(client, true);
            return;
        }
        String address = client == null || client.getCurrentServerEntry() == null ? "" : client.getCurrentServerEntry().address;
        onJoin(client, address);
    }

    public boolean isSessionActive() {
        return this.sessionId != null;
    }

    public void onJoin(MinecraftClient client, String address) {
        if (!this.config.enabled || !isMinepieceAddress(address) || this.sessionId != null) {
            return;
        }
        this.sessionId = UUID.randomUUID().toString();
        this.serverAddress = address;
        this.sequence = 0L;
        this.sessionStartedMs = System.currentTimeMillis();
        this.droppedEvents.set(0L);
        resetObservationState();
        offer(new StartSessionWork(this.sessionId, this.sessionStartedMs, address));
        record(TelemetryEvent.Category.SESSION, "session_start", json("server", address));
        capturePlayerInventory(client, "session_start", true);
        if (this.config.discordEnabled && this.config.sessionAlerts) {
            enqueueDiscord("session_start", "Minepiece session started.", null);
        }
    }

    public void onDisconnect(MinecraftClient client) {
        finishSession(client, true);
    }

    public void onClientStopping(MinecraftClient client) {
        finishSession(client, true);
        this.running = false;
        offer(new StopWork());
        try {
            this.worker.join(10_000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public void tick(MinecraftClient client) {
        if (!active() || client.player == null) {
            return;
        }
        this.ticks++;
        boolean recentInventoryAction = this.lastSlotAction != null
            && System.currentTimeMillis() - this.lastSlotActionMs <= 2_500L;
        if (this.config.inventoryEnabled && (this.ticks % 20 == 0 || (recentInventoryAction && this.ticks % 2 == 0))) {
            captureContainer(client);
            capturePlayerInventory(client, "periodic", false);
        }
        if (this.config.combatEnabled) {
            captureCombatState(client);
        }
    }

    public void onIncomingPlayerChat(String message, String sender) {
        if (active() && this.config.chatCommandsEnabled) {
            JsonObject data = json("direction", "incoming_player", "message", message);
            data.addProperty("sender", sender == null ? "" : sender);
            record(TelemetryEvent.Category.CHAT_COMMAND, "chat", data);
        }
    }

    public void onIncomingGameMessage(String message, boolean overlay) {
        if (active() && this.config.chatCommandsEnabled) {
            record(TelemetryEvent.Category.CHAT_COMMAND, overlay ? "actionbar" : "server_message",
                json("direction", "incoming_server", "message", message));
        }
    }

    public void onOutgoingChat(String message) {
        if (active() && this.config.chatCommandsEnabled) {
            record(TelemetryEvent.Category.CHAT_COMMAND, "chat", json("direction", "outgoing", "message", message));
        }
    }

    public void onOutgoingCommand(String command) {
        if (command == null) {
            return;
        }
        String normalized = command.trim().replaceFirst("^/", "");
        String lower = normalized.toLowerCase(Locale.ROOT);
        String root = lower.split("\\s+", 2)[0];
        if (root.equals("ec")) {
            this.pendingContainer = "ender_chest";
        } else if (lower.matches("is\\s+chest\\s+[0-9]+")) {
            this.pendingContainer = "island_chest_" + lower.replaceFirst("^is\\s+chest\\s+", "");
        } else if (root.equals("pets")) {
            this.pendingContainer = "pet_storage";
        } else if (root.equals("friuts") || root.equals("fruits")) {
            this.pendingContainer = "fruit_storage";
        } else if (root.equals("trash")) {
            this.pendingContainer = "trash";
        } else if (root.equals("ah") || root.equals("auctionhouse")) {
            this.pendingContainer = "auction_house";
        }
        if (active() && this.config.chatCommandsEnabled) {
            record(TelemetryEvent.Category.CHAT_COMMAND, "command",
                json("direction", "outgoing", "command", TelemetryRedactor.redactCommand(command)));
        }
    }

    public void onEconomyEvent(String kind, long signedAmount, String source, String rawMessage) {
        if (!active() || !this.config.economyEnabled) {
            return;
        }
        JsonObject data = json("kind", kind, "source", source, "message", rawMessage == null ? "" : rawMessage);
        data.addProperty("signed_amount", signedAmount);
        record(TelemetryEvent.Category.ECONOMY, "money_change", data);
    }

    public void onAttackEntity(Entity entity) {
        if (!active() || !this.config.combatEnabled || entity == null) {
            return;
        }
        this.lastAttackedEntity = entity;
        record(TelemetryEvent.Category.COMBAT, "attack_attempt", json(
            "target_type", Registries.ENTITY_TYPE.getId(entity.getType()).toString(),
            "target_name", entity.getName().getString(), "target_kind", entityKind(entity)
        ));
    }

    public void onUseBlock(MinecraftClient client, Hand hand, BlockHitResult hit) {
        if (!active() || !this.config.interactionsEnabled || client.world == null) {
            return;
        }
        record(TelemetryEvent.Category.INTERACTION, "use_block", json(
            "hand", hand.name(), "block", Registries.BLOCK.getId(client.world.getBlockState(hit.getBlockPos()).getBlock()).toString()
        ));
    }

    public void onAttackBlock(MinecraftClient client, BlockPos pos) {
        if (!active() || !this.config.interactionsEnabled || client.world == null) {
            return;
        }
        record(TelemetryEvent.Category.INTERACTION, "attack_block", json(
            "block", Registries.BLOCK.getId(client.world.getBlockState(pos).getBlock()).toString()
        ));
    }

    public void onUseEntity(Entity entity, Hand hand) {
        if (!active() || !this.config.interactionsEnabled || entity == null) {
            return;
        }
        record(TelemetryEvent.Category.INTERACTION, "use_entity", json(
            "hand", hand.name(), "entity_type", Registries.ENTITY_TYPE.getId(entity.getType()).toString(),
            "entity_name", entity.getName().getString(), "entity_kind", entityKind(entity)
        ));
    }

    public void onUseItem(ItemStack stack, Hand hand) {
        if (!active() || !this.config.interactionsEnabled) {
            return;
        }
        record(TelemetryEvent.Category.INTERACTION, "use_item", json(
            "hand", hand.name(), "item", itemId(stack),
            "item_name", stack == null || stack.isEmpty() ? "" : stack.getName().getString()
        ));
    }

    public void onSlotClick(int syncId, int slotId, SlotActionType actionType) {
        this.lastSlotAction = actionType;
        this.lastSlotActionMs = System.currentTimeMillis();
        if (active() && this.config.interactionsEnabled) {
            JsonObject data = json("action", actionType.name());
            data.addProperty("sync_id", syncId);
            data.addProperty("slot", slotId);
            record(TelemetryEvent.Category.INTERACTION, "inventory_click", data);
        }
    }

    public String status() {
        return "Telemetry=" + onOff(this.config.enabled) + ", session=" + (active() ? "active" : "inactive")
            + ", storage=" + this.storageStatus + ", Discord=" + onOff(this.config.discordEnabled)
            + ", outbox=" + this.pendingDiscord + ", dropped=" + this.droppedEvents.get();
    }

    public String exportCurrentSession() {
        if (!active()) {
            return "No active telemetry session.";
        }
        offer(new ExportWork(this.sessionId, false));
        return "Current-session XLSX export queued.";
    }

    public String exportAllHistory() {
        offer(new ExportWork(null, false));
        return "All-history XLSX export queued.";
    }

    public String testDiscord() {
        if (!this.config.discordEnabled) {
            return "Discord delivery is disabled.";
        }
        if (!DiscordWebhookClient.isValidWebhook(this.config.webhookUrl)) {
            return "Discord webhook is missing or invalid.";
        }
        enqueueDiscord("test", "Minepiece QoL telemetry webhook test.", null);
        return "Discord webhook test queued.";
    }

    public static boolean isMinepieceAddress(String address) {
        if (address == null) {
            return false;
        }
        String host = address.trim().toLowerCase(Locale.ROOT);
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(':') == colon) {
            host = host.substring(0, colon);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return host.equals("play.minepiece.net") || host.endsWith(".play.minepiece.net");
    }

    private void finishSession(MinecraftClient client, boolean clean) {
        if (!active()) {
            return;
        }
        String endingSession = this.sessionId;
        if (this.config.inventoryEnabled) {
            capturePlayerInventory(client, "session_end", true);
        }
        record(TelemetryEvent.Category.SESSION, "session_end", json("clean", Boolean.toString(clean)));
        offer(new FinishSessionWork(endingSession, System.currentTimeMillis(), clean, this.droppedEvents.get(), this.config.discordEnabled));
        this.sessionId = null;
        resetObservationState();
    }

    private boolean active() {
        return this.sessionId != null;
    }

    private void capturePlayerInventory(MinecraftClient client, String reason, boolean forceSnapshot) {
        if (!active() || !this.config.inventoryEnabled || client.player == null) {
            return;
        }
        Map<Integer, ItemSnapshot> now = new LinkedHashMap<>();
        for (int slot = 0; slot < client.player.getInventory().size(); slot++) {
            now.put(slot, snapshot(client.player.getInventory().getStack(slot)));
        }
        recordInventoryChanges("player", reason, this.playerInventory, now, forceSnapshot,
            this.activeContainer, this.activeContainerName);
        this.playerInventory = now;
    }

    private void captureContainer(MinecraftClient client) {
        if (!(client.currentScreen instanceof HandledScreen<?> screen) || client.player == null) {
            this.currentContainerScreen = null;
            this.activeContainer = "";
            this.activeContainerName = "";
            this.containerInventory = Map.of();
            return;
        }
        boolean first = this.currentContainerScreen != screen;
        if (first) {
            this.currentContainerScreen = screen;
            String inferred = inferContainer(screen);
            this.activeContainer = this.pendingContainer.isBlank() ? inferred : this.pendingContainer;
            this.activeContainerName = displayContainerName(this.activeContainer, screen.getTitle().getString());
            this.activeContainer = pageSpecificLocation(this.activeContainer, screen.getTitle().getString());
            this.containerInventory = this.knownContainers.getOrDefault(this.activeContainer, Map.of());
            this.pendingContainer = "";
        }
        if (!isTrackedStorage(this.activeContainer)) {
            return;
        }
        Map<Integer, ItemSnapshot> now = new LinkedHashMap<>();
        int containerSlot = 0;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.inventory != client.player.getInventory()) {
                ItemSnapshot value = snapshot(slot.getStack());
                if (shouldIgnoreMenuItem(this.activeContainer, value)) value = ItemSnapshot.EMPTY;
                now.put(containerSlot++, value);
            }
        }
        boolean firstObservation = !this.knownContainers.containsKey(this.activeContainer);
        recordInventoryChanges(this.activeContainer, firstObservation ? "container_open" : "container_change",
            this.containerInventory, now, firstObservation, "", this.activeContainerName);
        this.containerInventory = now;
        this.knownContainers.put(this.activeContainer, now);
    }

    private void recordInventoryChanges(String inventory, String reason, Map<Integer, ItemSnapshot> before,
                                        Map<Integer, ItemSnapshot> after, boolean forceSnapshot,
                                        String contextInventory, String inventoryName) {
        for (Map.Entry<Integer, ItemSnapshot> entry : after.entrySet()) {
            ItemSnapshot old = before.getOrDefault(entry.getKey(), ItemSnapshot.EMPTY);
            ItemSnapshot value = entry.getValue();
            if (forceSnapshot || !value.equals(old)) {
                JsonObject data = value.toJson();
                data.addProperty("inventory", inventory);
                data.addProperty("reason", reason);
                data.addProperty("slot", entry.getKey());
                data.addProperty("previous_count", old.count());
                data.addProperty("previous_item_id", old.itemId());
                data.addProperty("previous_name", old.name());
                data.addProperty("previous_signature", old.signature());
                data.addProperty("previous_container_contents_known", old.containerKnown());
                data.add("previous_contents", old.contentsJson());
                data.addProperty("inventory_name", inventoryName == null ? "" : inventoryName);
                data.addProperty("context_inventory", contextInventory == null ? "" : contextInventory);
                data.addProperty("context_name", this.activeContainerName);
                if (this.lastSlotAction != null && System.currentTimeMillis() - this.lastSlotActionMs <= 2_500L) {
                    data.addProperty("interaction_action", this.lastSlotAction.name());
                }
                record(TelemetryEvent.Category.INVENTORY, forceSnapshot ? "slot_snapshot" : "slot_change", data);
            }
        }
    }

    private static String inferContainer(HandledScreen<?> screen) {
        String title = screen.getTitle().getString();
        if (screen.getScreenHandler() instanceof ShulkerBoxScreenHandler) {
            return "opened_shulker:" + slug(title.isBlank() ? "shulker" : title);
        }
        return "opened_container:" + slug(title.isBlank() ? "container" : title);
    }

    private static String displayContainerName(String location, String screenTitle) {
        if (location.startsWith("opened_shulker:")) return screenTitle.isBlank() ? "Shulker" : screenTitle;
        if (location.startsWith("opened_container:")) return screenTitle.isBlank() ? "Opened container" : screenTitle;
        return switch (location) {
            case "ender_chest" -> "Ender Chest";
            case "pet_storage" -> "Pet Storage";
            case "fruit_storage" -> "Fruit Storage";
            case "trash" -> "Trash";
            case "auction_house" -> "Auction House";
            default -> location.startsWith("island_chest_")
                ? "Island Chest " + location.substring("island_chest_".length()) : screenTitle;
        };
    }

    private static String pageSpecificLocation(String location, String title) {
        if (!(location.equals("pet_storage") || location.equals("fruit_storage"))) return location;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)(?:page|pagina|página)\\s*[:#-]?\\s*(\\d+)").matcher(title);
        return matcher.find() ? location + "_page_" + matcher.group(1) : location;
    }

    private static boolean isTrackedStorage(String location) {
        return !location.isBlank() && !location.equals("trash") && !location.equals("auction_house");
    }

    private static boolean shouldIgnoreMenuItem(String location, ItemSnapshot value) {
        if (!(location.startsWith("pet_storage") || location.startsWith("fruit_storage")) || value.count() <= 0) {
            return false;
        }
        String name = value.name().toLowerCase(Locale.ROOT).trim();
        if (value.itemId().endsWith("stained_glass_pane") && (name.isBlank() || name.equals(" "))) return true;
        return name.matches(".*\\b(next|previous|back|close|search|sort|filter|help|info|page)\\b.*");
    }

    private static String slug(String raw) {
        String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("§.", "")
            .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return value.isBlank() ? "container" : value;
    }

    private void captureCombatState(MinecraftClient client) {
        float health = client.player.getHealth();
        float absorption = client.player.getAbsorptionAmount();
        if (!Float.isNaN(this.lastHealth) && (health != this.lastHealth || absorption != this.lastAbsorption)) {
            JsonObject data = new JsonObject();
            data.addProperty("previous_health", this.lastHealth);
            data.addProperty("health", health);
            data.addProperty("previous_absorption", this.lastAbsorption);
            data.addProperty("absorption", absorption);
            record(TelemetryEvent.Category.COMBAT, "vitals_change", data);
        }
        boolean dead = client.player.isDead();
        if (dead && !this.wasDead) {
            record(TelemetryEvent.Category.COMBAT, "death", new JsonObject());
            if (this.config.discordEnabled && this.config.deathAlerts) {
                enqueueDiscord("death", "Player death observed during Minepiece session.", null);
            }
        } else if (!dead && this.wasDead) {
            record(TelemetryEvent.Category.COMBAT, "respawn", new JsonObject());
        }
        if (this.lastAttackedEntity != null && !this.lastAttackedEntity.isAlive() && !this.lastAttackedEntity.isRemoved()) {
            record(TelemetryEvent.Category.COMBAT, "observed_target_death", json(
                "target_type", Registries.ENTITY_TYPE.getId(this.lastAttackedEntity.getType()).toString(),
                "target_name", this.lastAttackedEntity.getName().getString(),
                "target_kind", entityKind(this.lastAttackedEntity)
            ));
            this.lastAttackedEntity = null;
        }
        this.lastHealth = health;
        this.lastAbsorption = absorption;
        this.wasDead = dead;
    }

    private void resetObservationState() {
        this.ticks = 0;
        this.lastHealth = Float.NaN;
        this.lastAbsorption = Float.NaN;
        this.wasDead = false;
        this.lastAttackedEntity = null;
        this.playerInventory = Map.of();
        this.currentContainerScreen = null;
        this.pendingContainer = "";
        this.activeContainer = "";
        this.activeContainerName = "";
        this.containerInventory = Map.of();
        this.knownContainers.clear();
        this.lastSlotAction = null;
        this.lastSlotActionMs = 0L;
    }

    private void record(TelemetryEvent.Category category, String type, JsonObject data) {
        String id = this.sessionId;
        if (id == null) {
            return;
        }
        TelemetryEvent event = new TelemetryEvent(id, ++this.sequence, System.currentTimeMillis(), category, type, GSON.toJson(data));
        if (!this.queue.offer(new EventWork(event))) {
            long dropped = this.droppedEvents.incrementAndGet();
            if (dropped == 1L && this.config.discordEnabled && this.config.overflowAlerts) {
                enqueueDiscord("overflow", "Telemetry event queue overflowed; some events were dropped.", null);
            }
        }
    }

    private void enqueueDiscord(String kind, String content, Path attachment) {
        if (this.config.discordEnabled) {
            offer(new DiscordWork(kind, content, "", attachment == null ? "" : attachment.toString()));
        }
    }

    private void offer(Work work) {
        if (!this.queue.offer(work)) {
            this.droppedEvents.incrementAndGet();
        }
    }

    private void workerLoop() {
        TelemetryDatabase database = null;
        try {
            database = new TelemetryDatabase(this.configManager.databasePath());
            database.recoverInterruptedSessions(System.currentTimeMillis());
            this.storageStatus = "ready";
            List<TelemetryEvent> batch = new ArrayList<>(100);
            while (this.running || !this.queue.isEmpty()) {
                Work work = this.queue.poll(1, TimeUnit.SECONDS);
                try {
                    if (work instanceof EventWork eventWork) {
                        batch.add(eventWork.event());
                        while (batch.size() < 100 && this.queue.peek() instanceof EventWork) {
                            batch.add(((EventWork) this.queue.poll()).event());
                        }
                        database.insertEvents(batch);
                        batch.clear();
                    } else if (work != null) {
                        processWork(database, work);
                    }
                    deliverNext(database);
                    this.pendingDiscord = database.pendingOutboxCount();
                    this.storageStatus = "ready";
                } catch (Exception operationFailure) {
                    batch.clear();
                    this.storageStatus = "error: " + operationFailure.getClass().getSimpleName();
                    if (this.config.discordEnabled && this.config.failureAlerts) {
                        try {
                            database.enqueueDiscord(System.currentTimeMillis(), "storage_failure", "Telemetry storage operation failed.", "");
                        } catch (SQLException ignored) {
                        }
                    }
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            this.storageStatus = "interrupted";
        } catch (Exception exception) {
            this.storageStatus = "error: " + exception.getClass().getSimpleName();
        } finally {
            if (database != null) {
                try {
                    database.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void processWork(TelemetryDatabase database, Work work) throws Exception {
        if (work instanceof StartSessionWork start) {
            database.startSession(start.id(), start.startedMs(), start.server(), "1.21.11", "1.1.0");
        } else if (work instanceof FinishSessionWork finish) {
            database.finishSession(finish.id(), finish.endedMs(), finish.clean(), finish.dropped());
            Path safeAttachment = null;
            TelemetrySessionReport safeReport = null;
            boolean exportFailed = false;
            try {
                TelemetryWorkbookExporter.export(database, this.configManager.exportsDir(), finish.id(), false);
            } catch (Exception exportFailure) {
                exportFailed = true;
            }
            if (finish.sendDiscord()) {
                try {
                    safeAttachment = TelemetryWorkbookExporter.export(database, this.configManager.exportsDir(), finish.id(), true);
                } catch (Exception exportFailure) {
                    exportFailed = true;
                }
                try {
                    safeReport = TelemetrySessionReport.build(database, finish.id(), true, java.time.ZoneId.systemDefault());
                } catch (Exception reportFailure) {
                    exportFailed = true;
                }
            }
            if (exportFailed && this.config.discordEnabled && this.config.failureAlerts) {
                database.enqueueDiscord(System.currentTimeMillis(), "export_failure", "Telemetry XLSX export failed.", "");
            }
            if (finish.sendDiscord()) {
                boolean attachmentReady = sendableAttachment(safeAttachment);
                String payloadJson = safeReport == null ? "" : TelemetryDiscordFormatter.sessionPayload(safeReport, attachmentReady);
                String fallback = safeReport == null ? "Minepiece session complete. Detailed report unavailable." : "";
                database.enqueueDiscord(System.currentTimeMillis(), "session_summary", fallback, payloadJson,
                    attachmentReady ? safeAttachment.toString() : "");
                if (!attachmentReady && safeAttachment != null) {
                    try {
                        Files.deleteIfExists(safeAttachment);
                    } catch (Exception ignored) {
                    }
                }
            }
        } else if (work instanceof ExportWork export) {
            TelemetryWorkbookExporter.export(database, this.configManager.exportsDir(), export.sessionId(), export.discordSafe());
        } else if (work instanceof DiscordWork discordWork) {
            database.enqueueDiscord(System.currentTimeMillis(), discordWork.kind(), discordWork.content(),
                discordWork.payloadJson(), discordWork.attachmentPath());
        }
    }

    private void deliverNext(TelemetryDatabase database) throws SQLException {
        if (!this.config.discordEnabled || !DiscordWebhookClient.isValidWebhook(this.config.webhookUrl)) {
            return;
        }
        TelemetryDatabase.OutboxEntry entry = database.nextOutbox(System.currentTimeMillis());
        if (entry == null) {
            return;
        }
        Path attachment = entry.attachmentPath().isBlank() ? null : Path.of(entry.attachmentPath());
        if (attachment != null) {
            try {
                if (!Files.isRegularFile(attachment) || Files.size(attachment) > DiscordWebhookClient.MAX_ATTACHMENT_BYTES) {
                    attachment = null;
                }
            } catch (Exception ignored) {
                attachment = null;
            }
        }
        String payloadJson = entry.payloadJson();
        if (attachment == null && !entry.attachmentPath().isBlank() && "session_summary".equals(entry.kind())) {
            payloadJson = TelemetryDiscordFormatter.markAttachmentUnavailable(payloadJson);
        }
        DiscordWebhookClient.DeliveryResult result = this.discord.send(
            this.config.webhookUrl, entry.content(), payloadJson, attachment);
        if (result.success()) {
            database.completeOutbox(entry.id());
            if (!entry.attachmentPath().isBlank()) {
                try {
                    Files.deleteIfExists(Path.of(entry.attachmentPath()));
                } catch (Exception ignored) {
                }
            }
            return;
        }
        int attempts = entry.attempts() + 1;
        long delay = result.retryAfterMs() > 0L ? result.retryAfterMs()
            : attempts <= RETRY_DELAYS_MS.length ? RETRY_DELAYS_MS[attempts - 1] : 3_600_000L;
        database.failOutbox(entry.id(), attempts, System.currentTimeMillis() + delay, result.error());
    }

    private static ItemSnapshot snapshot(ItemStack stack) {
        return snapshot(stack, 0);
    }

    private static ItemSnapshot snapshot(ItemStack stack, int depth) {
        if (stack == null || stack.isEmpty()) {
            return ItemSnapshot.EMPTY;
        }
        List<ContainedItem> contents = new ArrayList<>();
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container != null && depth < 2) {
            DefaultedList<ItemStack> contained = DefaultedList.ofSize(27, ItemStack.EMPTY);
            container.copyTo(contained);
            for (int slot = 0; slot < contained.size(); slot++) {
                if (!contained.get(slot).isEmpty()) contents.add(new ContainedItem(slot, snapshot(contained.get(slot), depth + 1)));
            }
        }
        String components = stack.getComponents().toString();
        String signature = Integer.toHexString(stack.getComponents().hashCode());
        return new ItemSnapshot(itemId(stack), stack.getName().getString(), stack.getCount(), stack.getDamage(),
            components, signature, container != null, List.copyOf(contents));
    }

    private static String itemId(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "minecraft:air" : Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static String entityKind(Entity entity) {
        if (entity == null) return "entity";
        return "minecraft:player".equals(Registries.ENTITY_TYPE.getId(entity.getType()).toString()) ? "player" : "mob";
    }

    private static boolean sendableAttachment(Path path) {
        try {
            return path != null && Files.isRegularFile(path) && Files.size(path) <= DiscordWebhookClient.MAX_ATTACHMENT_BYTES;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static JsonObject json(String... pairs) {
        JsonObject object = new JsonObject();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            object.addProperty(pairs[i], pairs[i + 1]);
        }
        return object;
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }

    private sealed interface Work permits EventWork, StartSessionWork, FinishSessionWork, ExportWork, DiscordWork, StopWork {
    }

    private record EventWork(TelemetryEvent event) implements Work {
    }

    private record StartSessionWork(String id, long startedMs, String server) implements Work {
    }

    private record FinishSessionWork(String id, long endedMs, boolean clean, long dropped, boolean sendDiscord) implements Work {
    }

    private record ExportWork(String sessionId, boolean discordSafe) implements Work {
    }

    private record DiscordWork(String kind, String content, String payloadJson, String attachmentPath) implements Work {
    }

    private record StopWork() implements Work {
    }

    private record ItemSnapshot(String itemId, String name, int count, int damage, String components,
                                String signature, boolean containerKnown, List<ContainedItem> contents) {
        private static final ItemSnapshot EMPTY = new ItemSnapshot("minecraft:air", "", 0, 0, "{}", "", false, List.of());

        JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("item_id", this.itemId);
            object.addProperty("name", this.name);
            object.addProperty("count", this.count);
            object.addProperty("damage", this.damage);
            JsonObject componentData = new JsonObject();
            componentData.addProperty("serialized", this.components);
            object.add("components", componentData);
            object.addProperty("signature", this.signature);
            object.addProperty("container_contents_known", this.containerKnown);
            object.add("contents", contentsJson());
            return object;
        }

        JsonArray contentsJson() {
            JsonArray array = new JsonArray();
            for (ContainedItem content : this.contents) {
                JsonObject value = content.item().toJson();
                value.addProperty("slot", content.slot());
                array.add(value);
            }
            return array;
        }
    }

    private record ContainedItem(int slot, ItemSnapshot item) {
    }
}
