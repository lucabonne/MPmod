package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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

    public BossTracker(PersistentState state, Consumer<PersistentState> stateSaver, DebugLogManager debugLogManager) {
        this.state = state;
        this.stateSaver = stateSaver;
        this.debugLogManager = debugLogManager;
    }

    public void onTablistFooter(String footerText) {
        Optional<String> parsed = ChatParsers.parseSpawnId(footerText);
        String newSpawnId = parsed.orElse("");
        if (!this.currentSpawnId.equals(newSpawnId)) {
            this.currentSpawnId = newSpawnId;
        }
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
        boolean removedOtherSpawns = clearOtherSpawnEntries(effectiveSpawnId);
        String bossName = data.bossName() == null ? "" : data.bossName().trim();
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

        boolean pruned = pruneSpawnEntries(effectiveSpawnId, existingKey);
        if (changed || pruned || removedOtherSpawns) {
            this.stateSaver.accept(this.state);
        }
    }

    public void onBossKill(String bossName) {
        String cleanName = bossName == null ? "" : bossName.trim();
        if (cleanName.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        PersistentState.BossSpawnState entry = resolveTrackedBoss(cleanName);
        if (entry != null) {
            entry.bossName = cleanName;
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
        boolean removedOtherSpawns = clearOtherSpawnEntries(spawnId);
        String key = buildBossKey(spawnId, cleanName, 0, 0, 0);

        PersistentState.BossSpawnState newEntry = new PersistentState.BossSpawnState();
        newEntry.spawnId = spawnId;
        newEntry.bossName = cleanName;
        newEntry.x = 0;
        newEntry.y = 0;
        newEntry.z = 0;
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
            "[BOSS] chat add name=%s spawn=%s timer=%ds pruned=%s removedOtherSpawns=%s",
            cleanName,
            spawnId,
            CHAT_BOSS_CYCLE_SECONDS,
            pruned,
            removedOtherSpawns
        ));
    }

    public void captureMinibossActionbar(String actionbar, int x, int y, int z) {
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
        String canonical = bossName == null
            ? ""
            : bossName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return CHAT_ADD_EXCLUDED_BOSSES.contains(canonical);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
            PersistentState.BossSpawnState value = entry.getValue();
            if (value.miniboss) {
                if (value.removeAfterEpochMs > 0L && now >= value.removeAfterEpochMs) {
                    keysToRemove.add(entry.getKey());
                }
                continue;
            }
            if (value.nextSpawnEpochMs > 0L && now >= value.nextSpawnEpochMs + NORMAL_BOSS_READY_AUTO_REMOVE_MS) {
                keysToRemove.add(entry.getKey());
            }
        }
        for (String key : keysToRemove) {
            this.state.bosses.remove(key);
            changed = true;
        }
        if (changed) {
            this.stateSaver.accept(this.state);
        }
    }

    public List<HudLine> getHudLines(boolean includeCoords) {
        long now = System.currentTimeMillis();
        List<PersistentState.BossSpawnState> entries = new ArrayList<>();
        if (!this.currentSpawnId.isBlank()) {
            for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
                if (this.currentSpawnId.equals(value.spawnId)) {
                    entries.add(value);
                }
            }
            if (entries.isEmpty()) {
                for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
                    if (GLOBAL_SPAWN_ID.equals(value.spawnId)) {
                        entries.add(value);
                    }
                }
            }
        } else {
            entries.addAll(this.state.bosses.values());
        }
        entries.sort(Comparator.comparingLong(entry -> entry.nextSpawnEpochMs));

        List<HudLine> lines = new ArrayList<>();
        for (PersistentState.BossSpawnState entry : entries) {
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
            String label = (entry.bossName == null || entry.bossName.isBlank() ? "Boss " + entry.spawnId : entry.bossName)
                + " - " + (ready ? "READY" : NumberParser.formatTimer(remaining));
            lines.add(new HudLine(label, color));
            if (entry.miniboss || includeCoords) {
                lines.add(new HudLine(String.format(Locale.ROOT, "  WP %d %d %d", entry.x, entry.y, entry.z), waypointColor));
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
        List<PersistentState.BossSpawnState> entries = new ArrayList<>(this.state.bosses.values());
        entries.sort(Comparator
            .comparing((PersistentState.BossSpawnState entry) -> entry.spawnId == null ? "" : entry.spawnId)
            .thenComparingLong(entry -> entry.nextSpawnEpochMs));

        List<String> lines = new ArrayList<>();
        lines.add("Current spawn: " + (this.currentSpawnId.isBlank() ? "<none>" : this.currentSpawnId));
        if (entries.isEmpty()) {
            lines.add("No bosses recorded.");
            return lines;
        }

        long now = System.currentTimeMillis();
        for (PersistentState.BossSpawnState entry : entries) {
            String timer = entry.nextSpawnEpochMs <= 0L
                ? "UNKNOWN"
                : entry.nextSpawnEpochMs <= now
                    ? "READY"
                    : NumberParser.formatTimer(entry.nextSpawnEpochMs - now);
            String name = entry.bossName == null || entry.bossName.isBlank() ? "<unnamed>" : entry.bossName;
            String spawnId = entry.spawnId == null || entry.spawnId.isBlank() ? "<none>" : entry.spawnId;
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
        for (PersistentState.BossSpawnState entry : getDisplayedWaypointEntries()) {
            if (!entry.miniboss) {
                continue;
            }
            if (entry.removeAfterEpochMs > 0L && now >= entry.removeAfterEpochMs) {
                continue;
            }
            long remaining = Math.max(0L, entry.nextSpawnEpochMs - now);
            waypoints.add(new MinibossWaypoint(
                entry.bossName == null || entry.bossName.isBlank() ? "Miniboss" : entry.bossName,
                entry.minibossSymbol == null ? "" : entry.minibossSymbol,
                entry.x,
                entry.y,
                entry.z,
                remaining
            ));
        }
        return waypoints;
    }

    private PersistentState.BossSpawnState resolveTrackedBoss(String bossName) {
        if (!this.currentSpawnId.isBlank()) {
            for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
                if (this.currentSpawnId.equals(value.spawnId) && bossNameMatches(value.bossName, bossName)) {
                    return value;
                }
            }
            for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
                if (GLOBAL_SPAWN_ID.equals(value.spawnId) && bossNameMatches(value.bossName, bossName)) {
                    return value;
                }
            }
        }

        for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
            if (bossNameMatches(value.bossName, bossName)) {
                return value;
            }
        }
        this.debugLogManager.logInternal("Boss kill detected without tracked spawn: " + bossName);
        return null;
    }

    private String findBossKey(String spawnId, String bossName, int x, int y, int z) {
        String normalizedTarget = normalizeBossName(bossName);
        for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
            PersistentState.BossSpawnState value = entry.getValue();
            if (!spawnId.equals(value.spawnId)) {
                continue;
            }
            if (value.x == x && value.y == y && value.z == z && normalizeBossName(value.bossName).equals(normalizedTarget)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean clearOtherSpawnEntries(String keepSpawnId) {
        boolean removed = false;
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, PersistentState.BossSpawnState> entry : this.state.bosses.entrySet()) {
            if (!keepSpawnId.equals(entry.getValue().spawnId)) {
                keysToRemove.add(entry.getKey());
            }
        }
        for (String key : keysToRemove) {
            this.state.bosses.remove(key);
            removed = true;
        }
        return removed;
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
        String tracked = normalizeBossName(trackedName);
        String incoming = normalizeBossName(incomingName);
        if (tracked.isBlank() || incoming.isBlank()) {
            return false;
        }
        return tracked.equals(incoming) || tracked.contains(incoming) || incoming.contains(tracked);
    }

    private static String buildBossKey(String spawnId, String bossName, int x, int y, int z) {
        String normalized = normalizeBossName(bossName);
        if (normalized.isBlank()) {
            normalized = "unknown";
        }
        return spawnId + "|" + normalized + "|" + x + "|" + y + "|" + z;
    }

    private static String normalizeBossName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isCompleteBossData(TooltipParsers.BossTooltipData data) {
        if (data == null) {
            return false;
        }
        return data.cycleSeconds() > 0;
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

    private List<PersistentState.BossSpawnState> getDisplayedWaypointEntries() {
        List<PersistentState.BossSpawnState> entries = new ArrayList<>();
        if (!this.currentSpawnId.isBlank()) {
            for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
                if (this.currentSpawnId.equals(value.spawnId)) {
                    entries.add(value);
                }
            }
            if (entries.isEmpty()) {
                for (PersistentState.BossSpawnState value : this.state.bosses.values()) {
                    if (GLOBAL_SPAWN_ID.equals(value.spawnId)) {
                        entries.add(value);
                    }
                }
            }
        } else {
            entries.addAll(this.state.bosses.values());
        }
        entries.sort(Comparator.comparingLong(entry -> entry.nextSpawnEpochMs));
        return entries;
    }

    public record HudLine(String text, int color) {
    }

    public record MinibossWaypoint(String name, String symbol, int x, int y, int z, long remainingMs) {
    }

    private static final class SymbolProgress {
        int lastValue;
        long lastSeenMs;
    }
}
