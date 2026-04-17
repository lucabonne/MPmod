package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

public final class ScrollTracker {
    private static final Pattern SCROLL_NAME_PATTERN = Pattern.compile("\\bscroll\\b(?:\\s*\\(([^)]+)\\))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern OBJECTIVE_PATTERN = Pattern.compile("^objective\\s*:?\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("\\(([0-9][0-9.,]*)\\s*/\\s*([0-9][0-9.,]*)\\)");
    private static final Pattern DIGITS_ONLY = Pattern.compile("[^0-9]");
    private static final int MAX_OBJECTIVE_CHARS = 32;
    private static final int COLOR_COMMON = 0xFF71D17A;
    private static final int COLOR_RARE = 0xFF6AA8FF;
    private static final int COLOR_EPIC = 0xFFC48AFF;
    private static final int COLOR_LEGENDARY = 0xFFFFD96B;
    private static final int COLOR_MYTHIC = 0xFFFF6262;
    private static final int COLOR_HEADER = 0xFFFFD6DF;
    private static final String SCROLL_MYTHIC_MARKER = "愈潮";
    private static final List<String> SCROLL_TYPE_ORDER = List.of("Fisherman", "Farmer", "Miner", "Lumberjack");

    private final DebugLogManager debugLogManager;

    private List<ScrollEntry> entries = List.of();
    private String lastFingerprint = "";

    public ScrollTracker(DebugLogManager debugLogManager) {
        this.debugLogManager = debugLogManager;
    }

    public void clear() {
        if (this.entries.isEmpty() && this.lastFingerprint.isBlank()) {
            return;
        }
        this.entries = List.of();
        this.lastFingerprint = "";
    }

    public void scanInventory(PlayerEntity player) {
        if (player == null) {
            clear();
            return;
        }

        List<ScrollEntry> parsed = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            String name = TextUtil.normalize(stack.getName());
            if (!looksLikeScrollName(name)) {
                continue;
            }

            Optional<ScrollEntry> entry = parseScroll(player, stack, name);
            if (entry.isEmpty()) {
                continue;
            }

            parsed.add(entry.get());
        }

        String fingerprint = buildFingerprint(parsed);
        if (!fingerprint.equals(this.lastFingerprint)) {
            this.lastFingerprint = fingerprint;
            this.entries = List.copyOf(parsed);
            if (!parsed.isEmpty()) {
                this.debugLogManager.logInternal("[SCROLLS] updated: " + parsed.size() + " tracked in inventory");
            }
        }
    }

    public List<BossTracker.HudLine> getHudLines() {
        return getHudLines(null);
    }

    public List<BossTracker.HudLine> getHudLines(Function<String, String> localizer) {
        if (this.entries.isEmpty()) {
            return List.of();
        }

        Map<String, List<ScrollEntry>> grouped = new LinkedHashMap<>();
        for (String type : SCROLL_TYPE_ORDER) {
            grouped.put(type, new ArrayList<>());
        }
        for (ScrollEntry entry : this.entries) {
            grouped.computeIfAbsent(entry.type(), key -> new ArrayList<>()).add(entry);
        }

        List<BossTracker.HudLine> lines = new ArrayList<>(this.entries.size() * 2);
        for (Map.Entry<String, List<ScrollEntry>> bucket : grouped.entrySet()) {
            List<ScrollEntry> quests = bucket.getValue();
            if (quests.isEmpty()) {
                continue;
            }
            lines.add(new BossTracker.HudLine(localizeScrollType(bucket.getKey(), localizer) + ":", COLOR_HEADER));
            for (ScrollEntry quest : quests) {
                lines.add(new BossTracker.HudLine("  - " + quest.questLine(), colorForRarity(quest.rarity())));
            }
        }
        return lines;
    }

    private static Optional<ScrollEntry> parseScroll(PlayerEntity player, ItemStack stack, String normalizedName) {
        List<Text> tooltip;
        try {
            tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, player, TooltipType.BASIC);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
        if (tooltip == null || tooltip.isEmpty()) {
            return Optional.empty();
        }

        List<String> lines = TextUtil.normalizeLines(tooltip);
        String scrollType = extractScrollType(normalizedName);
        if (scrollType.isBlank() && !looksLikeScrollTooltip(lines)) {
            return Optional.empty();
        }
        if (scrollType.isBlank()) {
            scrollType = "Scroll";
        }

        String objective = extractObjective(lines);
        if (objective.isBlank()) {
            objective = "Objective";
        }

        long current = -1L;
        long needed = -1L;
        Matcher progressMatcher = PROGRESS_PATTERN.matcher(objective);
        if (!progressMatcher.find()) {
            for (String line : lines) {
                Matcher fallback = PROGRESS_PATTERN.matcher(line);
                if (fallback.find()) {
                    progressMatcher = fallback;
                    break;
                }
            }
        }

        String progressText = "";
        if (progressMatcher.find(0)) {
            current = parseLongDigits(progressMatcher.group(1));
            needed = parseLongDigits(progressMatcher.group(2));
            if (current >= 0L && needed > 0L) {
                int pct = (int) Math.max(0L, Math.min(100L, Math.round((current * 100.0D) / needed)));
                progressText = " " + current + "/" + needed + " (" + pct + "%)";
            } else {
                progressText = " " + progressMatcher.group(1) + "/" + progressMatcher.group(2);
            }
            objective = objective.replace(progressMatcher.group(0), "").trim();
            if (objective.isBlank()) {
                objective = "Objective";
            }
        }

        String questLine = shrink(objective, MAX_OBJECTIVE_CHARS) + progressText;
        RarityDetector.Rarity rarity = detectScrollRarity(stack, tooltip);
        return Optional.of(new ScrollEntry(scrollType, objective, current, needed, rarity, questLine));
    }

    private static String extractObjective(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = OBJECTIVE_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String inline = matcher.group(1) == null ? "" : matcher.group(1).trim();
            if (!inline.isBlank()) {
                return inline;
            }
            for (int j = i + 1; j < lines.size(); j++) {
                String next = lines.get(j);
                if (next == null || next.isBlank() || isRarityLine(next) || next.toLowerCase(Locale.ROOT).startsWith("expires")) {
                    continue;
                }
                return next.trim();
            }
            return "";
        }

        for (String line : lines) {
            if (line == null || line.isBlank() || isRarityLine(line)) {
                continue;
            }
            if (PROGRESS_PATTERN.matcher(line).find()) {
                return line;
            }
        }
        return "";
    }

    private static String extractScrollType(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return "";
        }
        Matcher matcher = SCROLL_NAME_PATTERN.matcher(normalizedName);
        if (!matcher.find()) {
            return "";
        }
        String inParens = matcher.group(1);
        if (inParens != null && !inParens.isBlank()) {
            return titleCase(inParens.trim());
        }
        return titleCase(normalizedName);
    }

    private static boolean looksLikeScrollName(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return false;
        }
        return SCROLL_NAME_PATTERN.matcher(normalizedName).find();
    }

    private static boolean looksLikeScrollTooltip(List<String> lines) {
        boolean hasObjective = false;
        boolean hasProgress = false;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("objective")) {
                hasObjective = true;
            }
            if (PROGRESS_PATTERN.matcher(line).find()) {
                hasProgress = true;
            }
        }
        return hasObjective && hasProgress;
    }

    private static boolean isRarityLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String upper = line.trim().toUpperCase(Locale.ROOT);
        return "COMMON".equals(upper)
            || "RARE".equals(upper)
            || "EPIC".equals(upper)
            || "LEGENDARY".equals(upper)
            || "MYTHIC".equals(upper)
            || "MYTHICAL".equals(upper);
    }

    private static long parseLongDigits(String token) {
        if (token == null || token.isBlank()) {
            return -1L;
        }
        String digits = DIGITS_ONLY.matcher(token).replaceAll("");
        if (digits.isBlank()) {
            return -1L;
        }
        try {
            return Long.parseLong(digits);
        } catch (RuntimeException ignored) {
            return -1L;
        }
    }

    private static String shrink(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, Math.max(0, maxChars));
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    private static int colorForRarity(RarityDetector.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> COLOR_COMMON;
            case RARE -> COLOR_RARE;
            case EPIC -> COLOR_EPIC;
            case LEGENDARY -> COLOR_LEGENDARY;
            case MYTHIC -> COLOR_MYTHIC;
        };
    }

    private static RarityDetector.Rarity detectScrollRarity(ItemStack stack, List<Text> tooltip) {
        RarityDetector.Rarity detected = RarityDetector.detect(stack, tooltip).orElse(RarityDetector.Rarity.COMMON);
        if (detected == RarityDetector.Rarity.EPIC && containsMarker(tooltip, SCROLL_MYTHIC_MARKER)) {
            return RarityDetector.Rarity.MYTHIC;
        }
        return detected;
    }

    private static boolean containsMarker(List<Text> lines, String marker) {
        if (lines == null || lines.isEmpty() || marker == null || marker.isBlank()) {
            return false;
        }
        for (Text line : lines) {
            if (line != null && line.getString().contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lowered = value.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
    }

    private static String buildFingerprint(List<ScrollEntry> entries) {
        if (entries.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(entries.size() * 48);
        for (ScrollEntry entry : entries) {
            builder.append(entry.type()).append('|')
                .append(entry.objective()).append('|')
                .append(entry.current()).append('/')
                .append(entry.needed()).append('|')
                .append(entry.rarity()).append(';');
        }
        return builder.toString();
    }

    private static String localizeScrollType(String type, Function<String, String> localizer) {
        if (type == null || type.isBlank()) {
            return localized(localizer, "hud.panel.scrolls", "Scrolls");
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "fisherman" -> localized(localizer, "jobs.name.fisherman", "Fisherman");
            case "farmer" -> localized(localizer, "jobs.name.farmer", "Farmer");
            case "miner" -> localized(localizer, "jobs.name.miner", "Miner");
            case "lumberjack" -> localized(localizer, "jobs.name.lumberjack", "Lumberjack");
            case "scroll" -> localized(localizer, "hud.panel.scrolls", "Scrolls");
            default -> type;
        };
    }

    private static String localized(Function<String, String> localizer, String key, String fallback) {
        if (localizer == null) {
            return fallback;
        }
        String value = localizer.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }

    private record ScrollEntry(
        String type,
        String objective,
        long current,
        long needed,
        RarityDetector.Rarity rarity,
        String questLine
    ) {
    }
}
