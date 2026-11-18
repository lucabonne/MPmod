package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lock timers of the six Marineford safes, read from the text floating above each one. */
public final class SafeChestTracker {
    public record Safe(String key, int x, int y, int z, int color) { }
    public enum Status { UNKNOWN, LOCKED, READY, PICKED }

    public static final List<Safe> SAFES = List.of(
        new Safe("purple", -1086, 228, 5698, 0xFFB46CFF),
        new Safe("green", -1060, 211, 5741, 0xFF55FF55),
        new Safe("orange", -1002, 225, 5669, 0xFFFFAA33),
        new Safe("red", -1031, 254, 5624, 0xFFFF5555),
        new Safe("yellow", -951, 229, 5648, 0xFFFFFF55),
        new Safe("blue", -940, 211, 5743, 0xFF55AAFF));
    private static final Pattern LOCK_TIME = Pattern.compile("lock time:\\s*((?:\\d+\\s*[dhms]\\s*)+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PART = Pattern.compile("(\\d+)\\s*([dhms])", Pattern.CASE_INSENSITIVE);
    /** Hologram text sits a few blocks above its safe; safes are 40+ blocks apart. */
    private static final double HOLOGRAM_RANGE = 6;
    private static final double PICK_RANGE = 12;
    private static final double ISLAND_RANGE = 250;

    /** Same look as the bosses panel's spawn headers. */
    private static final int SPAWN_HEADER_COLOR = 0xFF97B9EA;
    /** The spawn the player is on: gold, plus a "»" and "(current)" so it still shows with a custom HUD text color. */
    private static final int CURRENT_SPAWN_HEADER_COLOR = 0xFFFFD36B;
    private static final String UNKNOWN_SPAWN = "";

    /** Saved as-is in {@link PersistentState#safeChests}. */
    public static final class SpawnSafes {
        Status[] status = new Status[SAFES.size()];
        long[] unlockAtMs = new long[SAFES.size()];
        SpawnSafes() { java.util.Arrays.fill(status, Status.UNKNOWN); }

        /** Repairs a saved entry with missing or wrong-length arrays. */
        private SpawnSafes repaired() {
            SpawnSafes fixed = new SpawnSafes();
            for (int i = 0; i < SAFES.size(); i++) {
                if (status != null && i < status.length && status[i] != null) fixed.status[i] = status[i];
                if (unlockAtMs != null && i < unlockAtMs.length) fixed.unlockAtMs[i] = unlockAtMs[i];
            }
            return fixed;
        }
    }

    private java.util.Map<String, SpawnSafes> saved;
    private Runnable save = () -> { };

    // Unknown spawn ("") sorts last, like the bosses panel.
    private final java.util.TreeMap<String, SpawnSafes> spawns = new java.util.TreeMap<>(
        java.util.Comparator.comparing((String id) -> id.isEmpty()).thenComparing(id -> id.toLowerCase(Locale.ROOT)));

    private SpawnSafes spawn(String spawnId) {
        return spawns.computeIfAbsent(spawnId == null ? UNKNOWN_SPAWN : spawnId.trim(), ignored -> new SpawnSafes());
    }

    /** Loads saved timers and writes every later change back to {@code saved}. */
    public void attachPersistence(java.util.Map<String, SpawnSafes> saved, Runnable save) {
        this.saved = saved;
        this.save = save;
        saved.forEach((id, safes) -> { if (id != null && safes != null) spawns.put(id.trim(), safes.repaired()); });
    }

    private void persist() {
        if (saved == null) return;
        saved.clear();
        saved.putAll(spawns);
        save.run();
    }

    public static OptionalLong parseLockSeconds(String text) {
        Matcher m = LOCK_TIME.matcher(text);
        if (!m.find()) return OptionalLong.empty();
        long seconds = 0;
        Matcher part = PART.matcher(m.group(1));
        while (part.find()) {
            long n = Long.parseLong(part.group(1));
            seconds += switch (Character.toLowerCase(part.group(2).charAt(0))) {
                case 'd' -> n * 86_400; case 'h' -> n * 3_600; case 'm' -> n * 60; default -> n;
            };
        }
        return OptionalLong.of(seconds);
    }

    /** Index of the safe whose hologram could be at this position, or -1. */
    public static int safeAt(double x, double y, double z) {
        return nearest(x, y, z, HOLOGRAM_RANGE);
    }

    public static boolean nearIsland(double x, double y, double z) {
        return nearest(x, y, z, ISLAND_RANGE) >= 0;
    }

    private static int nearest(double x, double y, double z, double range) {
        int best = -1;
        double bestSq = range * range;
        for (int i = 0; i < SAFES.size(); i++) {
            Safe s = SAFES.get(i);
            double dx = x - (s.x() + 0.5), dy = y - s.y(), dz = z - (s.z() + 0.5);
            double sq = dx * dx + dy * dy + dz * dz;
            if (sq <= bestSq) { bestSq = sq; best = i; }
        }
        return best;
    }

    /** Hologram text (all lines) seen above safe {@code index} on {@code spawnId}. */
    public void observe(String spawnId, int index, String text, long now) {
        if (index < 0 || index >= SAFES.size() || text == null) return;
        boolean ready = text.toLowerCase(Locale.ROOT).contains("awaiting lockpicking");
        OptionalLong seconds = parseLockSeconds(text);
        if (!ready && seconds.isEmpty()) return;
        SpawnSafes safes = spawn(spawnId);
        Status status = ready ? Status.READY : Status.LOCKED;
        long unlockAt = ready ? safes.unlockAtMs[index] : now + seconds.getAsLong() * 1000;
        // Scans repeat every half second; only save when something really changed (not the countdown ticking).
        boolean changed = safes.status[index] != status || Math.abs(safes.unlockAtMs[index] - unlockAt) > 5_000;
        safes.status[index] = status;
        safes.unlockAtMs[index] = unlockAt;
        if (changed) persist();
    }

    /** Chat message; a successful pick marks the safe nearest to the player. */
    public void onChatMessage(String spawnId, String message, double px, double py, double pz) {
        if (message == null || !message.toLowerCase(Locale.ROOT).contains("lock picking successful")) return;
        int index = nearest(px, py, pz, PICK_RANGE);
        if (index >= 0) {
            spawn(spawnId).status[index] = Status.PICKED;
            persist();
        }
    }

    public Status status(String spawnId, int index, long now) {
        SpawnSafes safes = spawns.get(spawnId == null ? UNKNOWN_SPAWN : spawnId.trim());
        if (safes == null) return Status.UNKNOWN;
        return safes.status[index] == Status.LOCKED && now >= safes.unlockAtMs[index] ? Status.READY : safes.status[index];
    }

    public long remainingSeconds(String spawnId, int index, long now) {
        SpawnSafes safes = spawns.get(spawnId == null ? UNKNOWN_SPAWN : spawnId.trim());
        return safes == null ? 0 : Math.max(0, (safes.unlockAtMs[index] - now + 999) / 1000);
    }

    /** Forget every spawn and timer; the panel hides until the player is back at the island. */
    public void clear() {
        spawns.clear();
        persist();
    }

    public boolean hasData() {
        return !spawns.isEmpty();
    }

    public record Line(String text, int color) { }

    /**
     * One "Spawn X" header plus six colored safe lines per spawn with data.
     * {@code currentSpawn} is also listed (all "?") when the player is at the island but nothing is read yet,
     * and its header is marked as the current spawn.
     * Spawns are ordered by their soonest running timer (spawns with none come after, then by name, unknown last);
     * inside a spawn: Ready, then running timers soonest first, then Picked, then unknown.
     */
    public List<Line> lines(Function<String, String> tr, long now, String currentSpawn, boolean nearIsland) {
        List<Line> lines = new ArrayList<>();
        String current = currentSpawn == null ? UNKNOWN_SPAWN : currentSpawn.trim();
        for (String id : orderedIds(now, nearIsland ? current : null)) {
            boolean here = !current.isEmpty() && id.equals(current);
            String header = tr.apply("bosses.spawn") + " " + (id.isEmpty() ? tr.apply("common.unknown") : id);
            lines.add(here
                ? new Line("» " + header + " (" + tr.apply("bosses.current") + ")", CURRENT_SPAWN_HEADER_COLOR)
                : new Line(header, SPAWN_HEADER_COLOR));
            for (int i : safeOrder(id, now)) {
                String value = switch (status(id, i, now)) {
                    case UNKNOWN -> "?";
                    case READY -> tr.apply("safes.ready");
                    case PICKED -> tr.apply("safes.picked");
                    case LOCKED -> formatDuration(remainingSeconds(id, i, now));
                };
                lines.add(new Line(tr.apply("safes." + SAFES.get(i).key()) + ": " + value, SAFES.get(i).color()));
            }
        }
        return lines;
    }

    /** Saved spawns in the panel's order: soonest running timer first, then by name, unknown ("") last. */
    public List<String> spawnIds(long now) {
        return orderedIds(now, null);
    }

    /** Seconds until the spawn's next running timer ends, or empty when none is running. */
    public OptionalLong nextUnlockSeconds(String spawnId, long now) {
        long at = soonestUnlockMs(spawnId == null ? UNKNOWN_SPAWN : spawnId.trim(), now);
        return at == Long.MAX_VALUE ? OptionalLong.empty() : OptionalLong.of(Math.max(0, (at - now + 999) / 1000));
    }

    /** Forget one spawn's safes and timers; the others are kept. */
    public boolean removeSpawn(String spawnId) {
        boolean removed = spawns.remove(spawnId == null ? UNKNOWN_SPAWN : spawnId.trim()) != null;
        if (removed) persist();
        return removed;
    }

    /** Saved spawns plus {@code extra} (when not null), sorted by soonest running timer; ties keep the name order. */
    private List<String> orderedIds(long now, String extra) {
        java.util.Set<String> names = new java.util.TreeSet<>(spawns.comparator());
        names.addAll(spawns.keySet());
        if (extra != null) names.add(extra);
        List<String> ids = new ArrayList<>(names);
        ids.sort(java.util.Comparator.comparingLong(id -> soonestUnlockMs(id, now))); // stable sort
        return ids;
    }

    /** When the spawn's next still-running timer ends; Ready, Picked and unknown safes don't count. */
    private long soonestUnlockMs(String spawnId, long now) {
        SpawnSafes safes = spawns.get(spawnId);
        long soonest = Long.MAX_VALUE;
        if (safes == null) return soonest;
        for (int i = 0; i < SAFES.size(); i++) {
            if (status(spawnId, i, now) == Status.LOCKED) soonest = Math.min(soonest, safes.unlockAtMs[i]);
        }
        return soonest;
    }

    /** Safe indexes in display order: Ready, running timers soonest first, Picked, unknown; ties keep the safe order. */
    private List<Integer> safeOrder(String spawnId, long now) {
        SpawnSafes safes = spawns.get(spawnId);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < SAFES.size(); i++) order.add(i);
        order.sort(java.util.Comparator
            .comparingInt((Integer i) -> switch (status(spawnId, i, now)) {
                case READY -> 0; case LOCKED -> 1; case PICKED -> 2; case UNKNOWN -> 3;
            })
            .thenComparingLong(i -> status(spawnId, i, now) == Status.LOCKED ? safes.unlockAtMs[i] : 0));
        return order;
    }

    public static String formatDuration(long seconds) {
        long h = seconds / 3600, m = seconds % 3600 / 60, s = seconds % 60;
        if (h > 0) return h + "h " + m + "m";
        return m > 0 ? m + "m " + s + "s" : s + "s";
    }
}
