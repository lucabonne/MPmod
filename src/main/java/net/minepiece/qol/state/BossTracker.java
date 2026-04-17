package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minepiece.qol.parse.ActionbarParser;
import net.minepiece.qol.parse.ChatParsers;
import net.minepiece.qol.parse.TooltipParsers;
import net.minepiece.qol.util.NumberParser;

public final class BossTracker {
    private static final int MAX_BOSSES_PER_SPAWN = 15;
    private static final int MINIBOSS_TIMER_SECONDS = 300;
    private static final int MINIBOSS_REMOVE_GRACE_SECONDS = 30;
    private static final long MINIBOSS_REGISTER_COOLDOWN_MS = 5_000L;
    private static final int CHAT_BOSS_CYCLE_SECONDS = 900;
    private static final long NORMAL_BOSS_READY_AUTO_REMOVE_MS = 120_000L;
    private static final String GLOBAL_SPAWN_ID = "__global__";
    private static final int HUD_SPAWN_HEADER_COLOR = 0xFF97B9EA;
    private static final Set<String> CHAT_ADD_EXCLUDED_BOSSES = Set.of(
        "tbone",
        "kizaru",
        "aokiji",
        "hina",
        "akainu"
    );

    private final PersistentState state;
    private final Consumer<PersistentState> stateSaver;
    private final DebugLogManager debugLogManager;
    private final Map<String, SymbolProgress> minibossProgress = new HashMap<>();

    private String currentSpawnId = "";
    private long lastMinibossProgressMs;
    private long lastMinibossRegisterMs;
    private boolean minibossRegistrationEnabled = true;

    public BossTracker(PersistentState state, Consumer<PersistentState> stateSaver, DebugLogManager debugLogManager) {
        this.state = state;
        this.stateSaver = stateSaver;
        this.debugLogManager = debugLogManager;
        normalizePersistedState();
    }

    public void onTablistFooter(String footerText) {
        this.currentSpawnId = ChatParsers.parseSpawnId(footerText).orElse("");
    }

    public void setMinibossRegistrationEnabled(boolean enabled) {
        this.minibossRegistrationEnabled = enabled;
    }

    public void captureTooltip(String fallbackName, List<String> tooltipLines) {
        Optional<TooltipParsers.BossTooltipData> parsed = TooltipParsers.parseBossTooltip(tooltipLines, fallbackName);
        parsed.ifPresent(this::captureParsedTooltip);
    }

    public void captureParsedTooltip(TooltipParsers.BossTooltipData data) {
        if (!isCompleteBossData(data)) {
            return;
        }

        String effectiveSpawnId = activeOrGlobalSpawnId();
        String bossName = canonicalizeBossDisplayName(data.bossName());
        if (isBossIgnored(bossName)) {
            return;
        }
        String bossKey = buildBossKey(effectiveSpawnId, bossName, data.x(), data.y(), data.z());
        String existingKey = findBossKey(effectiveSpawnId, bossName, data.x(), data.y(), data.z());
        PersistentState.BossSpawnState entry = existingKey == null ? null : this.state.bosses.get(existingKey);
        if (entry == null) {
            entry = new PersistentState.BossSpawnState();
            this.state.bosses.put(bossKey, entry);
            existingKey = bossKey;
        } else if (!bossKey.equals(existingKey)) {
            this.state.bosses.remove(existingKey);
            this.state.bosses.put(bossKey, entry);
            existingKey = bossKey;
        }

        long candidateNextSpawn = System.currentTimeMillis() + data.remainingSeconds() * 1000L;
        boolean changed = !effectiveSpawnId.equals(entry.spawnId)
            || !bossName.equals(entry.bossName)
            || entry.x != data.x()
            || entry.y != data.y()
            || entry.z != data.z()
            || entry.cycleSeconds != data.cycleSeconds()
            || Math.abs(entry.nextSpawnEpochMs - candidateNextSpawn) > 1_000L;

        entry.spawnId = effectiveSpawnId;
        entry.bossName = bossName;
        entry.x = data.x();
        entry.y = data.y();
        entry.z = data.z();
        entry.cycleSeconds = data.cycleSeconds();
        entry.nextSpawnEpochMs = candidateNextSpawn;
        entry.miniboss = false;
        entry.minibossSymbol = "";
        entry.removeAfterEpochMs = 0L;
        registerEncounteredBoss(bossName);

        boolean pruned = pruneSpawnEntries(effectiveSpawnId, existingKey);
        if (changed || pruned) {
            this.stateSaver.accept(this.state);
        }
    }

    public void onBossKill(String bossName) {
        onBossKill(bossName, 0, 0, 0, false);
    }

    public void onBossKill(String bossName, int fallbackX, int fallbackY, int fallbackZ) {
        onBossKill(bossName, fallbackX, fallbackY, fallbackZ, true);
    }

    private void onBossKill(String bossName, int fallbackX, int fallbackY, int fallbackZ, boolean hasFallbackCoords) {
        String cleanName = bossName == null ? "" : bossName.trim();
        if (cleanName.isBlank()) {
            return;
        }
        String displayName = canonicalizeBossDisplayName(cleanName);
        if (isBossIgnored(displayName)) {
            return;
        }

        long now = System.currentTimeMillis();
        registerEncounteredBoss(displayName);
        PersistentState.BossSpawnState entry = resolveTrackedBoss(cleanName);
        if (entry != null) {
            if (entry.bossName == null || entry.bossName.isBlank()) {
                entry.bossName = displayName;
            }
            if (!hasKnownCoords(entry) && hasFallbackCoords) {
                entry.x = fallbackX;
                entry.y = fallbackY;
                entry.z = fallbackZ;
            }
            entry.cycleSeconds = CHAT_BOSS_CYCLE_SECONDS;
            entry.nextSpawnEpochMs = now + CHAT_BOSS_CYCLE_SECONDS * 1000L;
            entry.miniboss = false;
            entry.minibossSymbol = "";
            entry.removeAfterEpochMs = 0L;
            this.stateSaver.accept(this.state);
            return;
        }

        if (isExcludedFromChatAdd(cleanName)) {
            this.debugLogManager.logInternal("[BOSS] chat add skipped (excluded): " + cleanName);
            return;
        }

        String spawnId = activeOrGlobalSpawnId();
        int x = hasFallbackCoords ? fallbackX : 0;
        int y = hasFallbackCoords ? fallbackY : 0;
        int z = hasFallbackCoords ? fallbackZ : 0;
        String key = buildBossKey(spawnId, displayName, x, y, z);

        PersistentState.BossSpawnState newEntry = new PersistentState.BossSpawnState();
        newEntry.spawnId = spawnId;
        newEntry.bossName = displayName;
        newEntry.x = x;
        newEntry.y = y;
        newEntry.z = z;
        newEntry.cycleSeconds = CHAT_BOSS_CYCLE_SECONDS;
        newEntry.nextSpawnEpochMs = now + CHAT_BOSS_CYCLE_SECONDS * 1000L;
        newEntry.miniboss = false;
        newEntry.minibossSymbol = "";
        newEntry.removeAfterEpochMs = 0L;
        this.state.bosses.put(key, newEntry);

        boolean pruned = pruneSpawnEntries(spawnId, key);
        this.stateSaver.accept(this.state);
        this.debugLogManager.logInternal(String.format(
            Locale.ROOT,
            "[BOSS] chat add name=%s spawn=%s timer=%ds pruned=%s",
            displayName,
            spawnId,
            CHAT_BOSS_CYCLE_SECONDS,
            pruned
        ));
    }

    public void captureMinibossActionbar(String actionbar, int x, int y, int z) {
        if (!this.minibossRegistrationEnabled) {
            return;
        }
        if (actionbar == null || actionbar.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastMinibossProgressMs > 8_000L) {
            this.minibossProgress.clear();
        }
        this.lastMinibossProgressMs = now;

        List<ActionbarParser.SymbolGain> gains = ActionbarParser.parseSymbolGains(actionbar);
        int bountyValue = -1;
        int largeOtherValue = 0;
        int largeOtherDelta = 0;
        String largeOtherValueSymbol = "";
        String largeOtherDeltaSymbol = "";
        for (ActionbarParser.SymbolGain gain : gains) {
            if ("军".equals(gain.symbol())) {
                bountyValue = (int) Math.round(gain.value());
                continue;
            }
            if (!isCandidateMinibossRewardSymbol(gain.symbol())) {
                continue;
            }

            int currentValue = (int) Math.round(gain.value());
            SymbolProgress symbolProgress = this.minibossProgress.computeIfAbsent(gain.symbol(), key -> new SymbolProgress());
            int previousSymbolValue = symbolProgress.lastValue;
            long previousSymbolSeenMs = symbolProgress.lastSeenMs;
            boolean symbolStaleWindow = previousSymbolSeenMs == 0L || now - previousSymbolSeenMs > 2_500L;
            int symbolDelta;
            if (symbolStaleWindow || currentValue < previousSymbolValue) {
                symbolDelta = currentValue;
            } else {
                symbolDelta = Math.max(0, currentValue - previousSymbolValue);
            }
            symbolProgress.lastValue = currentValue;
            symbolProgress.lastSeenMs = now;

            if (currentValue > largeOtherValue) {
                largeOtherValue = currentValue;
                largeOtherValueSymbol = gain.symbol();
            }
            if (symbolDelta > largeOtherDelta) {
                largeOtherDelta = symbolDelta;
                largeOtherDeltaSymbol = gain.symbol();
            }
        }

        if (bountyValue < 0) {
            return;
        }

        SymbolProgress progress = this.minibossProgress.computeIfAbsent("军", key -> new SymbolProgress());
        int previousValue = progress.lastValue;
        long previousSeenMs = progress.lastSeenMs;

        int bountyDelta;
        boolean staleWindow = previousSeenMs == 0L || now - previousSeenMs > 2_500L;
        if (staleWindow || bountyValue < previousValue) {
            bountyDelta = bountyValue;
        } else {
            bountyDelta = Math.max(0, bountyValue - previousValue);
        }

        progress.lastValue = bountyValue;
        progress.lastSeenMs = now;

        boolean hasLargeRewardChunk = largeOtherDelta >= 100 || largeOtherValue >= 100;
        boolean explicitPlusTenBounty = actionbar.contains("+10 军");
        boolean minibossBountyChunk = bountyDelta == 10 || (explicitPlusTenBounty && hasLargeRewardChunk);
        boolean bossBountyValue = bountyValue >= 50 || bountyDelta >= 50;
        boolean minibossDetected = hasLargeRewardChunk && minibossBountyChunk && !bossBountyValue;
        this.debugLogManager.logInternal(String.format(
            Locale.ROOT,
            "[MINIBOSS] bounty=%d prev=%d delta=%d miniChunk=%s bossBounty=%s rewardV=%s:%d rewardD=%s:%d detected=%s line=%s",
            bountyValue,
            previousValue,
            bountyDelta,
            minibossBountyChunk,
            bossBountyValue,
            largeOtherValueSymbol,
            largeOtherValue,
            largeOtherDeltaSymbol,
            largeOtherDelta,
            minibossDetected,
            actionbar
        ));

        if (!minibossDetected) {
            return;
        }
        long sinceLastRegister = now - this.lastMinibossRegisterMs;
        if (sinceLastRegister < MINIBOSS_REGISTER_COOLDOWN_MS) {
            this.debugLogManager.logInternal(String.format(
                Locale.ROOT,
                "[MINIBOSS] skipped by cooldown: %dms < %dms",
                sinceLastRegister,
                MINIBOSS_REGISTER_COOLDOWN_MS
            ));
            return;
        }

        this.lastMinibossRegisterMs = now;
        registerMinibossWaypoint("军", x, y, z, now);
    }

    private static boolean isCandidateMinibossRewardSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        return !"军".equals(symbol) && !"实".equals(symbol);
    }

    private static boolean isExcludedFromChatAdd(String bossName) {
        String canonical = canonicalBossNameKey(bossName).replace(" ", "");
        return CHAT_ADD_EXCLUDED_BOSSES.contains(canonical);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        boolean changed = this.state.bosses.entrySet().removeIf(entry -> {
            PersistentState.BossSpawnState value = entry.getValue();
            if (value.miniboss) {
                return value.removeAfterEpochMs > 0L && now >= value.removeAfterEpochMs;
            }
            return value.nextSpawnEpochMs > 0L && now >= value.nextSpawnEpochMs + NORMAL_BOSS_READY_AUTO_REMOVE_MS;
        });
        if (changed) {
            this.stateSaver.accept(this.state);
        }
    }

    public List<HudLine> getHudLines(boolean includeCoords) {
        return getHudLines(includeCoords, null);
    }

    public List<HudLine> getHudLines(boolean includeCoords, Function<String, String> localizer) {
        List<HudLine> lines = new ArrayList<>();
        lines.addAll(getZoneBossHudLines(includeCoords, localizer));
        lines.addAll(getMinibossHudLines(includeCoords, localizer));
        return lines;
    }

    public List<HudLine> getZoneBossHudLines(boolean includeCoords) {
        return getZoneBossHudLines(includeCoords, null);
    }

    public List<HudLine> getZoneBossHudLines(boolean includeCoords, Function<String, String> localizer) {
        long now = System.currentTimeMillis();
        List<HudLine> lines = new ArrayList<>();

        List<PersistentState.BossSpawnState> zoneBosses = new ArrayList<>();
        for (PersistentState.BossSpawnState entry : this.state.bosses.values()) {
            if (entry.miniboss || entry.nextSpawnEpochMs <= 0L) {
                continue;
            }
            if (isBossIgnored(entry.bossName)) {
                continue;
            }
            zoneBosses.add(entry);
        }
        zoneBosses.sort(Comparator
            .comparing((PersistentState.BossSpawnState entry) -> sortableSpawnId(entry.spawnId))
            .thenComparingLong(entry -> entry.nextSpawnEpochMs)
            .thenComparing(entry -> normalizeBossName(entry.bossName)));

        String activeSpawnGroup = null;
        for (PersistentState.BossSpawnState entry : zoneBosses) {
            String entrySpawnId = normalizedSpawnId(entry.spawnId);
            if (!entrySpawnId.equals(activeSpawnGroup)) {
                String spawnLabel = "unknown".equals(entrySpawnId)
                    ? localized(localizer, "common.unknown", "UNKNOWN")
                    : entrySpawnId;
                lines.add(new HudLine(localized(localizer, "bosses.spawn", "Spawn") + " " + spawnLabel, HUD_SPAWN_HEADER_COLOR));
                activeSpawnGroup = entrySpawnId;
            }

            long remaining = entry.nextSpawnEpochMs - now;
            boolean ready = remaining <= 0L;
            int color = ready ? 0xFFD08484 : 0xFFC36B6B; // muted red
            int waypointColor = 0xFFA85E5E;
            String label = "  " + toHudDisplayName(entry.bossName, localized(localizer, "bosses.boss", "Boss"))
                + " - " + (ready ? localized(localizer, "common.ready", "READY") : NumberParser.formatTimer(remaining));
            lines.add(new HudLine(label, color));
            if (includeCoords && hasKnownCoords(entry)) {
                lines.add(new HudLine(
                    String.format(Locale.ROOT, "    %s %d %d %d", localized(localizer, "bosses.wp", "WP"), entry.x, entry.y, entry.z),
                    waypointColor
                ));
            }
        }
        return lines;
    }

    public List<HudLine> getMinibossHudLines(boolean includeCoords) {
        return getMinibossHudLines(includeCoords, null);
    }

    public List<HudLine> getMinibossHudLines(boolean includeCoords, Function<String, String> localizer) {
        long now = System.currentTimeMillis();
        List<HudLine> lines = new ArrayList<>();
        for (PersistentState.BossSpawnState entry : getVisibleBossStates()) {
            if (!entry.miniboss) {
                continue;
            }
            if (entry.nextSpawnEpochMs <= 0L) {
                continue;
            }
            if (entry.miniboss && entry.removeAfterEpochMs > 0L && now >= entry.removeAfterEpochMs) {
                continue;
            }

            long remaining = entry.nextSpawnEpochMs - now;
            boolean ready = remaining <= 0L;
            int color;
            int waypointColor;
            if (entry.miniboss) {
                color = ready ? 0xFFD4AE73 : 0xFFC39A5D; // muted orange
                waypointColor = 0xFFAE8A59;
            } else {
                color = ready ? 0xFFD08484 : 0xFFC36B6B; // muted red
                waypointColor = 0xFFA85E5E;
            }
            String fallbackSpawn = normalizedSpawnId(entry.spawnId);
            if ("unknown".equals(fallbackSpawn)) {
                fallbackSpawn = localized(localizer, "common.unknown", "UNKNOWN");
            }
            String label = toHudDisplayName(entry.bossName, localized(localizer, "bosses.boss", "Boss") + " " + fallbackSpawn)
                + " - " + (ready ? localized(localizer, "common.ready", "READY") : NumberParser.formatTimer(remaining));
            lines.add(new HudLine(label, color));
            if (entry.miniboss || includeCoords) {
                lines.add(new HudLine(
                    String.format(Locale.ROOT, "  %s %d %d %d", localized(localizer, "bosses.wp", "WP"), entry.x, entry.y, entry.z),
                    waypointColor
                ));
            }
        }
        return lines;
    }

    public boolean hasTrackedBosses() {
        return !this.state.bosses.isEmpty();
    }

    public String getCurrentSpawnId() {
        return this.currentSpawnId;
    }

    public void clear() {
        if (this.state.bosses.isEmpty()) {
            return;
        }
        this.state.bosses.clear();
        this.stateSaver.accept(this.state);
    }

    public Set<String> getBossRegistry() {
        return this.state.bossRegistry;
    }

    public Set<String> getIgnoredBosses() {
        return this.state.ignoredBosses;
    }

    public void registerEncounteredBoss(String bossName) {
        if (bossName == null) {
            return;
        }
        String trimmed = canonicalizeBossDisplayName(bossName);
        if (trimmed.isBlank()) {
            return;
        }
        if (isBossIgnored(trimmed)) {
            return;
        }
        if (this.state.bossRegistry.add(trimmed)) {
            this.stateSaver.accept(this.state);
        }
    }

    public boolean removeFromRegistry(String bossName) {
        if (bossName == null) {
            return false;
        }
        String canonicalTarget = canonicalBossNameKey(bossName);
        boolean removed = this.state.bossRegistry.removeIf(name -> canonicalBossNameKey(name).equals(canonicalTarget));
        if (removed) {
            this.stateSaver.accept(this.state);
        }
        return removed;
    }

    public List<Map.Entry<String, PersistentState.BossSpawnState>> getActiveEntries() {
        List<Map.Entry<String, PersistentState.BossSpawnState>> entries = new ArrayList<>();
        for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
            if (entry.getValue().miniboss || !isBossIgnored(entry.getValue().bossName)) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator
            .comparing((Map.Entry<String, PersistentState.BossSpawnState> entry) -> sortableSpawnId(entry.getValue().spawnId))
            .thenComparingLong(entry -> entry.getValue().nextSpawnEpochMs)
            .thenComparing(entry -> normalizeBossName(entry.getValue().bossName)));
        return entries;
    }

    public boolean removeActiveBossByKey(String key) {
        if (key == null) {
            return false;
        }
        boolean removed = this.state.bosses.remove(key) != null;
        if (removed) {
            this.stateSaver.accept(this.state);
        }
        return removed;
    }

    public boolean removeSpawn(String spawnId) {
        if (spawnId == null || spawnId.isBlank()) {
            return false;
        }
        boolean removed = this.state.bosses.entrySet().removeIf(entry -> spawnId.equals(entry.getValue().spawnId));
        if (removed) {
            this.stateSaver.accept(this.state);
        }
        return removed;
    }

    public void addBossFromRegistry(String bossName, int x, int y, int z) {
        if (bossName == null || bossName.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        String spawnId = activeOrGlobalSpawnId();
        String displayName = canonicalizeBossDisplayName(bossName);
        if (isBossIgnored(displayName)) {
            return;
        }
        String key = buildBossKey(spawnId, displayName, x, y, z);
        PersistentState.BossSpawnState entry = new PersistentState.BossSpawnState();
        entry.spawnId = spawnId;
        entry.bossName = displayName;
        entry.x = x;
        entry.y = y;
        entry.z = z;
        entry.cycleSeconds = CHAT_BOSS_CYCLE_SECONDS;
        entry.nextSpawnEpochMs = now + CHAT_BOSS_CYCLE_SECONDS * 1000L;
        entry.miniboss = false;
        entry.minibossSymbol = "";
        entry.removeAfterEpochMs = 0L;
        this.state.bosses.put(key, entry);
        registerEncounteredBoss(displayName);
        this.stateSaver.accept(this.state);
    }

    public boolean clearDisplayedIndex(int oneBasedIndex) {
        if (oneBasedIndex <= 0) {
            return false;
        }

        List<Map.Entry<String, PersistentState.BossSpawnState>> entries = getDisplayedEntries();
        int index = oneBasedIndex - 1;
        if (index >= entries.size()) {
            return false;
        }

        this.state.bosses.remove(entries.get(index).getKey());
        this.stateSaver.accept(this.state);
        return true;
    }

    public List<String> commandLines() {
        return commandLines(null);
    }

    public List<String> commandLines(Function<String, String> localizer) {
        List<PersistentState.BossSpawnState> entries = new ArrayList<>();
        for (PersistentState.BossSpawnState entry : this.state.bosses.values()) {
            if (entry.miniboss || !isBossIgnored(entry.bossName)) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator
            .comparing((PersistentState.BossSpawnState entry) -> entry.spawnId == null ? "" : entry.spawnId)
            .thenComparingLong(entry -> entry.nextSpawnEpochMs));

        List<String> lines = new ArrayList<>();
        String currentSpawn = this.currentSpawnId.isBlank()
            ? localized(localizer, "cmd.common.none_value", "<none>")
            : this.currentSpawnId;
        lines.add(localized(localizer, "cmd.bosses.current_spawn", "Current spawn: %s").formatted(currentSpawn));
        if (entries.isEmpty()) {
            lines.add(localized(localizer, "cmd.bosses.none_recorded", "No bosses recorded."));
            return lines;
        }

        long now = System.currentTimeMillis();
        for (PersistentState.BossSpawnState entry : entries) {
            String timer = entry.nextSpawnEpochMs <= 0L
                ? localized(localizer, "common.unknown", "UNKNOWN")
                : entry.nextSpawnEpochMs <= now
                    ? localized(localizer, "common.ready", "READY")
                    : NumberParser.formatTimer(entry.nextSpawnEpochMs - now);
            String name = nameOrDefault(entry.bossName, localized(localizer, "cmd.bosses.unnamed", "<unnamed>"));
            String spawnId = nameOrDefault(entry.spawnId, localized(localizer, "cmd.common.none_value", "<none>"));
            lines.add(String.format(
                Locale.ROOT,
                "[%s] %s - %s @ %d %d %d (cycle %dm)",
                spawnId,
                name,
                timer,
                entry.x,
                entry.y,
                entry.z,
                Math.max(0, entry.cycleSeconds / 60)
            ));
        }
        return lines;
    }

    public List<MinibossWaypoint> getActiveMinibossWaypoints() {
        long now = System.currentTimeMillis();
        List<MinibossWaypoint> waypoints = new ArrayList<>();
        for (PersistentState.BossSpawnState entry : getVisibleBossStates()) {
            if (!entry.miniboss || (entry.removeAfterEpochMs > 0L && now >= entry.removeAfterEpochMs)) {
                continue;
            }
            waypoints.add(new MinibossWaypoint(
                nameOrDefault(entry.bossName, "Miniboss"),
                entry.minibossSymbol == null ? "" : entry.minibossSymbol,
                entry.x, entry.y, entry.z,
                Math.max(0L, entry.nextSpawnEpochMs - now)
            ));
        }
        return waypoints;
    }

    public List<BossWaypoint> getActiveBossWaypoints() {
        long now = System.currentTimeMillis();
        List<BossWaypoint> waypoints = new ArrayList<>();
        for (PersistentState.BossSpawnState entry : getVisibleBossStates()) {
            if (entry.miniboss || !hasKnownCoords(entry) || isBossIgnored(entry.bossName)) {
                continue;
            }
            waypoints.add(new BossWaypoint(
                nameOrDefault(entry.bossName, "Boss"),
                entry.x, entry.y, entry.z,
                Math.max(0L, entry.nextSpawnEpochMs - now)
            ));
        }
        return waypoints;
    }

    public boolean ignoreBossForever(String bossName) {
        if (bossName == null || bossName.isBlank()) {
            return false;
        }
        String canonicalDisplay = canonicalizeBossDisplayName(bossName);
        if (canonicalDisplay.isBlank()) {
            return false;
        }
        String canonicalKey = canonicalBossNameKey(canonicalDisplay);
        if (canonicalKey.isBlank()) {
            return false;
        }

        if (this.state.ignoredBosses == null) {
            this.state.ignoredBosses = new LinkedHashSet<>();
        }

        boolean changed = this.state.ignoredBosses.add(canonicalDisplay);
        boolean removedRegistry = this.state.bossRegistry.removeIf(name -> canonicalBossNameKey(name).equals(canonicalKey));
        boolean removedActive = this.state.bosses.entrySet().removeIf(entry ->
            !entry.getValue().miniboss && canonicalBossNameKey(entry.getValue().bossName).equals(canonicalKey));
        if (changed || removedRegistry || removedActive) {
            this.stateSaver.accept(this.state);
            return true;
        }
        return false;
    }

    private PersistentState.BossSpawnState resolveTrackedBoss(String bossName) {
        if (!this.currentSpawnId.isBlank()) {
            for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
                if (isBossIgnored(value.bossName)) {
                    continue;
                }
                if (this.currentSpawnId.equals(value.spawnId) && bossNameMatches(value.bossName, bossName)) {
                    return value;
                }
            }
            for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
                if (isBossIgnored(value.bossName)) {
                    continue;
                }
                if (GLOBAL_SPAWN_ID.equals(value.spawnId) && bossNameMatches(value.bossName, bossName)) {
                    return value;
                }
            }
            this.debugLogManager.logInternal("[BOSS] kill did not match current spawn entry: " + bossName);
            return null;
        }

        for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
            if (isBossIgnored(value.bossName)) {
                continue;
            }
            if (bossNameMatches(value.bossName, bossName)) {
                return value;
            }
        }
        this.debugLogManager.logInternal("Boss kill detected without tracked spawn: " + bossName);
        return null;
    }

    private String findBossKey(String spawnId, String bossName, int x, int y, int z) {
        String normalizedTarget = canonicalBossNameKey(bossName);
        for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
            PersistentState.BossSpawnState value = entry.getValue();
            if (!spawnId.equals(value.spawnId)) {
                continue;
            }
            if (value.x == x && value.y == y && value.z == z
                && canonicalBossNameKey(value.bossName).equals(normalizedTarget)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean pruneSpawnEntries(String spawnId, String keepKey) {
        boolean removed = false;
        while (countSpawnEntries(spawnId) > MAX_BOSSES_PER_SPAWN) {
            String removableKey = null;
            for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
                if (!spawnId.equals(entry.getValue().spawnId)) {
                    continue;
                }
                if (entry.getKey().equals(keepKey)) {
                    continue;
                }
                removableKey = entry.getKey();
                break;
            }
            if (removableKey == null) {
                break;
            }
            this.state.bosses.remove(removableKey);
            removed = true;
        }
        return removed;
    }

    private int countSpawnEntries(String spawnId) {
        int count = 0;
        for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
            if (spawnId.equals(value.spawnId)) {
                count++;
            }
        }
        return count;
    }

    private static boolean bossNameMatches(String trackedName, String incomingName) {
        String tracked = canonicalBossNameKey(trackedName);
        String incoming = canonicalBossNameKey(incomingName);
        if (tracked.isBlank() || incoming.isBlank()) {
            return false;
        }
        return tracked.equals(incoming);
    }

    private static String buildBossKey(String spawnId, String bossName, int x, int y, int z) {
        String normalized = canonicalBossNameKey(bossName);
        if (normalized.isBlank()) {
            normalized = "unknown";
        }
        return spawnId + "|" + normalized + "|" + x + "|" + y + "|" + z;
    }

    private static String normalizeBossName(String value) {
        String lower = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static String canonicalBossNameKey(String value) {
        String normalized = normalizeBossName(value);
        if (normalized.isBlank()) {
            return "";
        }
        if ("trafalgar d water law".equals(normalized)) {
            return "trafalgar d law";
        }
        if ("monkey d luffy".equals(normalized)) {
            return "luffy";
        }
        return normalized;
    }

    private static String canonicalizeBossDisplayName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        String canonical = canonicalBossNameKey(trimmed);
        if ("trafalgar d law".equals(canonical)) {
            return "Trafalgar D. Law";
        }
        return trimmed;
    }

    private static String toHudDisplayName(String bossName, String fallback) {
        String fullName = nameOrDefault(bossName, fallback);
        if (fullName.isBlank()) {
            return fallback;
        }

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 3) {
            String first = stripOuterPunctuation(parts[0]);
            String last = stripOuterPunctuation(parts[parts.length - 1]);
            if (!first.isBlank() && !last.isBlank()) {
                return Character.toUpperCase(first.charAt(0)) + ". " + last;
            }
        }

        if (fullName.length() <= 14) {
            return fullName;
        }

        if (parts.length >= 2) {
            String first = stripOuterPunctuation(parts[0]);
            String last = stripOuterPunctuation(parts[parts.length - 1]);
            if (!first.isBlank() && !last.isBlank()) {
                return Character.toUpperCase(first.charAt(0)) + ". " + last;
            }
        }
        return fullName;
    }

    private static String stripOuterPunctuation(String token) {
        if (token == null) {
            return "";
        }
        return token.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
    }

    private static String normalizedSpawnId(String spawnId) {
        if (spawnId == null || spawnId.isBlank() || GLOBAL_SPAWN_ID.equals(spawnId)) {
            return "unknown";
        }
        return spawnId;
    }

    private static String sortableSpawnId(String spawnId) {
        String normalized = normalizedSpawnId(spawnId);
        if ("unknown".equals(normalized)) {
            return "zzzzzzzzzz";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static boolean isCompleteBossData(TooltipParsers.BossTooltipData data) {
        if (data == null) {
            return false;
        }
        return data.cycleSeconds() > 0;
    }

    private static String nameOrDefault(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name;
    }

    private static boolean hasKnownCoords(PersistentState.BossSpawnState entry) {
        return entry.x != 0 || entry.y != 0 || entry.z != 0;
    }

    private static String localized(Function<String, String> localizer, String key, String fallback) {
        if (localizer == null) {
            return fallback;
        }
        String value = localizer.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }

    private void normalizePersistedState() {
        boolean changed = false;
        Map<String, PersistentState.BossSpawnState> normalizedBosses = new LinkedHashMap<>();

        for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
            PersistentState.BossSpawnState value = entry.getValue();
            if (value == null) {
                continue;
            }

            String spawnId = value.spawnId == null || value.spawnId.isBlank() ? GLOBAL_SPAWN_ID : value.spawnId;
            if (!spawnId.equals(value.spawnId)) {
                value.spawnId = spawnId;
                changed = true;
            }

            if (!value.miniboss) {
                String canonicalDisplay = canonicalizeBossDisplayName(value.bossName);
                if (!canonicalDisplay.equals(value.bossName)) {
                    value.bossName = canonicalDisplay;
                    changed = true;
                }
            }

            String normalizedKey = value.miniboss
                ? buildBossKey(spawnId, "miniboss-" + (value.minibossSymbol == null ? "" : value.minibossSymbol), value.x, value.y, value.z)
                : buildBossKey(spawnId, value.bossName, value.x, value.y, value.z);
            PersistentState.BossSpawnState existing = normalizedBosses.get(normalizedKey);
            if (existing == null || preferCandidate(existing, value)) {
                normalizedBosses.put(normalizedKey, value);
            }
            if (!normalizedKey.equals(entry.getKey()) || existing != null) {
                changed = true;
            }
        }

        if (changed) {
            this.state.bosses = normalizedBosses;
        }

        Set<String> normalizedRegistry = new LinkedHashSet<>();
        Set<String> normalizedIgnored = new LinkedHashSet<>();
        if (this.state.ignoredBosses == null) {
            this.state.ignoredBosses = new LinkedHashSet<>();
        }
        for (String name : this.state.ignoredBosses) {
            String canonicalDisplay = canonicalizeBossDisplayName(name);
            if (!canonicalDisplay.isBlank()) {
                normalizedIgnored.add(canonicalDisplay);
            }
        }
        if (!normalizedIgnored.equals(this.state.ignoredBosses)) {
            this.state.ignoredBosses = normalizedIgnored;
            changed = true;
        }

        for (String name : this.state.bossRegistry) {
            String canonicalDisplay = canonicalizeBossDisplayName(name);
            if (!canonicalDisplay.isBlank() && !isBossIgnored(canonicalDisplay)) {
                normalizedRegistry.add(canonicalDisplay);
            }
        }
        boolean removedIgnoredActives = this.state.bosses.entrySet().removeIf(entry ->
            !entry.getValue().miniboss && isBossIgnored(entry.getValue().bossName));
        if (removedIgnoredActives) {
            changed = true;
        }

        for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
            if (value == null || value.miniboss) {
                continue;
            }
            String canonicalDisplay = canonicalizeBossDisplayName(value.bossName);
            if (!canonicalDisplay.equals(value.bossName)) {
                value.bossName = canonicalDisplay;
                changed = true;
            }
            if (!canonicalDisplay.isBlank() && !isBossIgnored(canonicalDisplay)) {
                normalizedRegistry.add(canonicalDisplay);
            }
        }
        if (!normalizedRegistry.equals(this.state.bossRegistry)) {
            this.state.bossRegistry = normalizedRegistry;
            changed = true;
        }

        if (changed) {
            this.stateSaver.accept(this.state);
        }
    }

    private static boolean preferCandidate(PersistentState.BossSpawnState existing, PersistentState.BossSpawnState candidate) {
        if (!hasKnownCoords(existing) && hasKnownCoords(candidate)) {
            return true;
        }
        return candidate.nextSpawnEpochMs > existing.nextSpawnEpochMs;
    }

    private void registerMinibossWaypoint(String symbol, int x, int y, int z, long now) {
        String spawnId = activeOrGlobalSpawnId();
        String minibossName = "Miniboss " + symbol;
        String existingKey = findMinibossKey(spawnId, symbol, x, y, z);
        String bossKey = buildBossKey(spawnId, "miniboss-" + symbol, x, y, z);

        PersistentState.BossSpawnState entry = existingKey == null ? null : this.state.bosses.get(existingKey);
        if (entry == null) {
            entry = new PersistentState.BossSpawnState();
            this.state.bosses.put(bossKey, entry);
            existingKey = bossKey;
        } else if (!existingKey.equals(bossKey)) {
            this.state.bosses.remove(existingKey);
            this.state.bosses.put(bossKey, entry);
            existingKey = bossKey;
        }

        entry.spawnId = spawnId;
        entry.bossName = minibossName;
        entry.x = x;
        entry.y = y;
        entry.z = z;
        entry.cycleSeconds = MINIBOSS_TIMER_SECONDS;
        entry.nextSpawnEpochMs = now + MINIBOSS_TIMER_SECONDS * 1000L;
        entry.miniboss = true;
        entry.minibossSymbol = symbol;
        entry.removeAfterEpochMs = entry.nextSpawnEpochMs + MINIBOSS_REMOVE_GRACE_SECONDS * 1000L;

        boolean pruned = pruneSpawnEntries(spawnId, existingKey);
        this.stateSaver.accept(this.state);
        this.debugLogManager.logInternal(String.format(
            Locale.ROOT,
            "[MINIBOSS] symbol=%s coords=(%d,%d,%d) timer=%ds removeAfter=%ds pruned=%s",
            symbol,
            x,
            y,
            z,
            MINIBOSS_TIMER_SECONDS,
            MINIBOSS_TIMER_SECONDS + MINIBOSS_REMOVE_GRACE_SECONDS,
            pruned
        ));
    }

    private String findMinibossKey(String spawnId, String symbol, int x, int y, int z) {
        for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
            PersistentState.BossSpawnState value = entry.getValue();
            if (!value.miniboss || !spawnId.equals(value.spawnId) || !symbol.equals(value.minibossSymbol)) {
                continue;
            }
            if (value.x == x && value.y == y && value.z == z) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String activeOrGlobalSpawnId() {
        return this.currentSpawnId.isBlank() ? GLOBAL_SPAWN_ID : this.currentSpawnId;
    }

    private boolean isBossIgnored(String bossName) {
        String canonical = canonicalBossNameKey(bossName);
        if (canonical.isBlank()) {
            return false;
        }
        for (String ignored : this.state.ignoredBosses) {
            if (canonicalBossNameKey(ignored).equals(canonical)) {
                return true;
            }
        }
        return false;
    }

    private List<Map.Entry<String, PersistentState.BossSpawnState>> getDisplayedEntries() {
        List<Map.Entry<String, PersistentState.BossSpawnState>> entries = new ArrayList<>();
        if (!this.currentSpawnId.isBlank()) {
            for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
                if (this.currentSpawnId.equals(entry.getValue().spawnId)) {
                    entries.add(entry);
                }
            }
            if (entries.isEmpty()) {
                for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
                    if (GLOBAL_SPAWN_ID.equals(entry.getValue().spawnId)) {
                        entries.add(entry);
                    }
                }
            }
        } else {
            entries.addAll(this.state.bosses.entrySet());
        }
        entries.sort(Comparator.comparingLong(entry -> entry.getValue().nextSpawnEpochMs));
        return entries;
    }

    private List<PersistentState.BossSpawnState> getVisibleBossStates() {
        List<Map.Entry<String, PersistentState.BossSpawnState>> entries = getDisplayedEntries();
        List<PersistentState.BossSpawnState> states = new ArrayList<>(entries.size());
        for (Map.Entry<String, PersistentState.BossSpawnState> entry : entries) {
            states.add(entry.getValue());
        }
        return states;
    }

    public record HudLine(String text, int color) {
    }

    public record MinibossWaypoint(String name, String symbol, int x, int y, int z, long remainingMs) {
    }

    public record BossWaypoint(String name, int x, int y, int z, long remainingMs) {
    }

    private static final class SymbolProgress {
        int lastValue;
        long lastSeenMs;
    }
}
