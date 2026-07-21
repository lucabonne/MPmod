package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.util.LocalizedText;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public final class InventoryXpTracker {
    private static final long SCAN_INTERVAL_MS = 350L;
    private static final int PARSE_CACHE_MAX = 256;
    private static final String NUMBER_TOKEN = "[0-9][0-9., \\u00a0]*";

    private static final Pattern LEVEL_PATTERN =
        Pattern.compile("\\b(?:lvl|lv|level|niveau|nivel|stufe|livello|poziom|seviye)\\s*[:.]?\\s*(\\d{1,4})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_LEVEL_SUFFIX_PATTERN =
        Pattern.compile("^(.*?)\\s*\\((?:lvl|lv|level|niveau|nivel|stufe|livello|poziom|seviye)\\s*[:.]?\\s*(\\d{1,4})\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern XP_PROGRESS_PATTERN =
        Pattern.compile("\\b(?:xp|experience|exp|erfahrung|esperienza|experiencia|doswiadczenie|doświadczenie|pengalaman|deneyim)\\b[^0-9]{0,16}(" + NUMBER_TOKEN + ")\\s*/\\s*(" + NUMBER_TOKEN + ")", Pattern.CASE_INSENSITIVE);
    private static final Pattern XP_PAIR_PATTERN =
        Pattern.compile("\\b(" + NUMBER_TOKEN + ")\\b\\D+\\b(" + NUMBER_TOKEN + ")\\b");

    private static final Pattern ASC_READY_PATTERN =
        Pattern.compile("\\b(?:ascension|ascension|ascenso|aufstieg|ascesa|wzniesienie|awans|kenaikan|yukselis|yükseliş)\\b.*\\b(?:available|disponible|verfugbar|verfügbar|disponibile|dostepne|dostępne|tersedia|hazir|hazır)\\b|\\b(?:available|disponible|verfugbar|verfügbar|disponibile|dostepne|dostępne|tersedia|hazir|hazır)\\b.*\\b(?:ascension|ascenso|aufstieg|ascesa|wzniesienie|awans|kenaikan|yukselis|yükseliş)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAXED_PATTERN =
        Pattern.compile("\\b(?:max(?:ed|imum)?(?:\\s*level)?|level\\s*max|lvl\\s*max|niveau\\s*max|nivel\\s*max|stufe\\s*max|livello\\s*max|poziom\\s*max|seviye\\s*max|maksymalny|maksimum|massimo)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_HEADER_PATTERN =
        Pattern.compile("^(?:obtain|use|information|effect|obtenir|utiliser|informations|effet|obtener|usar|informacion|información|efecto|erhalten|benutzen|information|effekt|ottenere|usa|informazioni|effetto|obter|usar|informacao|informação|efeito|zdobadz|zdobądź|uzyj|użyj|informacje|efekt|dapatkan|gunakan|informasi|efek|elde et|kullan|bilgi|etki)\\b", Pattern.CASE_INSENSITIVE);

    private static final List<String> FRUIT_KEYWORDS = List.of(
        "fruit",
        "devil fruit",
        "/fruit",
        "fruit du demon",
        "fruit du démon",
        "fruta del diablo",
        "teufelsfrucht",
        "frutto del diavolo",
        "fruta do diabo",
        "diabelski owoc",
        "buah iblis",
        "seytan meyvesi",
        "şeytan meyvesi"
    );
    private static final List<String> WEAPON_KEYWORDS = List.of(
        "weapon",
        "/weapon",
        "sword",
        "katana",
        "dagger",
        "bow",
        "gun",
        "staff",
        "spear",
        "arme",
        "epee",
        "épée",
        "espada",
        "waffe",
        "arma",
        "bron",
        "broń",
        "senjata",
        "silah",
        "kilic",
        "kılıç"
    );

    private final DebugLogManager debugLogManager;
    private List<TrackedItem> items = List.of();
    private String lastFingerprint = "";
    private long nextScanAtMs = 0L;
    private final Map<String, Optional<TrackedItem>> parseCache = new HashMap<>();

    public InventoryXpTracker(DebugLogManager debugLogManager) {
        this.debugLogManager = debugLogManager;
    }

    public void clear() {
        if (this.items.isEmpty() && this.lastFingerprint.isBlank()) {
            return;
        }
        this.items = List.of();
        this.lastFingerprint = "";
        this.nextScanAtMs = 0L;
        this.parseCache.clear();
    }

    public void scanInventory(PlayerEntity player) {
        if (player == null) {
            clear();
            return;
        }

        long now = System.currentTimeMillis();
        if (now < this.nextScanAtMs) {
            return;
        }
        this.nextScanAtMs = now + SCAN_INTERVAL_MS;

        List<TrackedItem> parsed = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String cacheKey = buildStackCacheKey(stack);
            Optional<TrackedItem> cached = this.parseCache.get(cacheKey);
            if (cached == null) {
                cached = parseTrackableItem(player, stack);
                putBounded(this.parseCache, cacheKey, cached);
            }
            cached.ifPresent(parsed::add);
        }

        String fingerprint = buildFingerprint(parsed);
        if (!fingerprint.equals(this.lastFingerprint)) {
            this.lastFingerprint = fingerprint;
            this.items = List.copyOf(parsed);
            if (!parsed.isEmpty()) {
                this.debugLogManager.logInternal("[INVXP] updated: " + parsed.size() + " fruit/weapon items tracked");
            }
        }
    }

    public List<String> getHudLines() {
        return getHudLines(null);
    }

    public List<String> getHudLines(Function<String, String> localizer) {
        if (this.items.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>(this.items.size());
        for (TrackedItem item : this.items) {
            String levelText = item.level() > 0 ? Integer.toString(item.level()) : "?";
            String line = item.name() + " " + levelText;
            if (item.progressState() == ProgressState.ASC_READY) {
                line += " " + localized(localizer, "xp.asc_ready", "asc ready");
            } else if (item.progressState() == ProgressState.MAXED) {
                line += " " + localized(localizer, "xp.maxed", "maxed");
            } else if (item.currentXp() >= 0L && item.neededXp() > 0L) {
                line += " " + item.currentXp() + "/" + item.neededXp();
            }
            lines.add(line);
        }
        return lines;
    }

    private static Optional<TrackedItem> parseTrackableItem(PlayerEntity player, ItemStack stack) {
        String rawName = TextUtil.normalize(stack.getName());
        String name = stripLevelSuffix(rawName);
        if (name.isBlank()) {
            return Optional.empty();
        }

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
        ItemCategory category = detectCategory(name, lines);
        int level = extractLevel(rawName, lines);
        List<String> progressionLines = extractProgressionLines(lines);
        XpProgress xpProgress = extractXp(progressionLines);
        ProgressState state = detectProgressState(lines, progressionLines, xpProgress);

        if (category == null) {
            return Optional.empty();
        }
        if (level <= 0 && xpProgress.current() < 0L && state == ProgressState.NORMAL) {
            return Optional.empty();
        }

        return Optional.of(new TrackedItem(
            category,
            name,
            level,
            xpProgress.current(),
            xpProgress.needed(),
            state
        ));
    }

    private static String buildStackCacheKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()) + "|"
            + stack.getCount() + "|"
            + TextUtil.normalize(stack.getName()) + "|"
            + stack.getComponents().hashCode();
    }

    private static void putBounded(Map<String, Optional<TrackedItem>> cache, String key, Optional<TrackedItem> value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (cache.size() >= PARSE_CACHE_MAX) {
            cache.clear();
        }
        cache.put(key, value);
    }

    private static ItemCategory detectCategory(String normalizedName, List<String> lines) {
        String lowerName = normalizedName.toLowerCase(Locale.ROOT);
        boolean fruit = containsAnyKeyword(lowerName, FRUIT_KEYWORDS);
        boolean weapon = containsAnyKeyword(lowerName, WEAPON_KEYWORDS);

        if (!fruit || !weapon) {
            for (String line : lines) {
                String lower = line.toLowerCase(Locale.ROOT);
                fruit |= containsAnyKeyword(lower, FRUIT_KEYWORDS);
                weapon |= containsAnyKeyword(lower, WEAPON_KEYWORDS);
            }
        }

        if (fruit) {
            return ItemCategory.FRUIT;
        }
        if (weapon) {
            return ItemCategory.WEAPON;
        }
        return null;
    }

    private static boolean containsAnyKeyword(String text, List<String> keywords) {
        return LocalizedText.containsAny(text, keywords.toArray(String[]::new));
    }

    private static int extractLevel(String normalizedName, List<String> lines) {
        for (String line : lines) {
            Matcher matcher = LEVEL_PATTERN.matcher(line);
            if (matcher.find()) {
                return parsePositiveInt(matcher.group(1));
            }
        }

        Matcher suffixMatcher = NAME_LEVEL_SUFFIX_PATTERN.matcher(normalizedName);
        if (suffixMatcher.matches()) {
            return parsePositiveInt(suffixMatcher.group(2));
        }

        Matcher nameMatcher = LEVEL_PATTERN.matcher(normalizedName);
        if (nameMatcher.find()) {
            return parsePositiveInt(nameMatcher.group(1));
        }
        return -1;
    }

    private static XpProgress extractXp(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = XP_PROGRESS_PATTERN.matcher(line);
            if (matcher.find()) {
                long current = parseLongDigits(matcher.group(1));
                long needed = parseLongDigits(matcher.group(2));
                if (current >= 0L && needed > 0L) {
                    return new XpProgress(current, needed);
                }
            }

            matcher = XP_PAIR_PATTERN.matcher(line);
            if (matcher.find()) {
                long current = parseLongDigits(matcher.group(1));
                long needed = parseLongDigits(matcher.group(2));
                if (current >= 0L && needed > 0L) {
                    return new XpProgress(current, needed);
                }
            }
        }
        return new XpProgress(-1L, -1L);
    }

    private static ProgressState detectProgressState(List<String> allLines, List<String> progressionLines, XpProgress xpProgress) {
        for (String line : progressionLines) {
            if (ASC_READY_PATTERN.matcher(line).find()) {
                return ProgressState.ASC_READY;
            }
        }

        if (xpProgress.current() >= 0L && xpProgress.needed() > 0L) {
            return ProgressState.NORMAL;
        }

        for (String line : progressionLines) {
            if (looksLikeStarsLine(line)) {
                return ProgressState.MAXED;
            }
        }

        for (String line : allLines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (ASC_READY_PATTERN.matcher(lower).find()) {
                return ProgressState.ASC_READY;
            }
            if (MAXED_PATTERN.matcher(lower).find()) {
                return ProgressState.MAXED;
            }
        }

        return ProgressState.NORMAL;
    }

    private static List<String> extractProgressionLines(List<String> lines) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line != null && LocalizedText.containsAny(line, "progression", "progres", "progrès", "progreso", "fortschritt", "progressione", "postep", "postęp", "kemajuan", "ilerleme")) {
                start = i;
                break;
            }
        }

        if (start < 0) {
            return lines;
        }

        List<String> progression = new ArrayList<>();
        for (int i = start + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            if (SECTION_HEADER_PATTERN.matcher(line.trim()).find()) {
                break;
            }
            progression.add(line);
        }
        return progression;
    }

    private static String stripLevelSuffix(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        Matcher matcher = NAME_LEVEL_SUFFIX_PATTERN.matcher(name);
        if (!matcher.matches()) {
            return name.trim();
        }
        String base = matcher.group(1);
        return base == null ? name.trim() : base.trim();
    }

    private static boolean looksLikeStarsLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }

        int stars = 0;
        int other = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (isStarGlyph(c)) {
                stars++;
            } else {
                other++;
            }
        }
        return stars >= 3 && other == 0;
    }

    private static boolean isStarGlyph(char c) {
        return c == '★' || c == '☆' || c == '⭐' || c == '✪' || c == '✯' || c == '✫'
            || c == '✦' || c == '✧' || c == '✰' || c == '✶' || c == '✷' || c == '✸' || c == '✹' || c == '✺';
    }

    private static int parsePositiveInt(String token) {
        if (token == null || token.isBlank()) {
            return -1;
        }
        try {
            int value = Integer.parseInt(token.trim());
            return value > 0 ? value : -1;
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static String localized(Function<String, String> localizer, String key, String fallback) {
        if (localizer == null) {
            return fallback;
        }
        String value = localizer.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }

    private static long parseLongDigits(String token) {
        if (token == null || token.isBlank()) {
            return -1L;
        }
        String digitsOnly = token.replaceAll("[^0-9]", "");
        if (digitsOnly.isBlank()) {
            return -1L;
        }
        try {
            return Long.parseLong(digitsOnly);
        } catch (RuntimeException ignored) {
            return -1L;
        }
    }

    private static String buildFingerprint(List<TrackedItem> parsed) {
        if (parsed.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(parsed.size() * 64);
        for (TrackedItem item : parsed) {
            sb.append(item.category().name())
                .append('|')
                .append(item.name())
                .append('|')
                .append(item.level())
                .append('|')
                .append(item.currentXp())
                .append('/')
                .append(item.neededXp())
                .append('|')
                .append(item.progressState().name())
                .append(';');
        }
        return sb.toString();
    }

    private enum ItemCategory {
        FRUIT,
        WEAPON
    }

    private enum ProgressState {
        NORMAL,
        ASC_READY,
        MAXED
    }

    private record XpProgress(long current, long needed) {
    }

    private record TrackedItem(
        ItemCategory category,
        String name,
        int level,
        long currentXp,
        long neededXp,
        ProgressState progressState
    ) {
    }
}
