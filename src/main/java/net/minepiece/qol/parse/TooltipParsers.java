package net.minepiece.qol.parse;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.util.NumberParser;

public final class TooltipParsers {
    private static final Pattern SELLING_PATTERN = Pattern.compile("Selling price:\\s*([0-9.,]+)\\s*([KMB])?", Pattern.CASE_INSENSITIVE);
    private static final Pattern AVERAGE_PATTERN = Pattern.compile("Average price:\\s*([0-9.,]+)\\s*([KMB])?", Pattern.CASE_INSENSITIVE);
    private static final Pattern COORDS_PATTERN =
        Pattern.compile("Coord[^0-9-]*(-?\\d+)\\D+(-?\\d+)\\D+(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CYCLE_MINUTES_PATTERN =
        Pattern.compile("\\(?\\s*(?:Every\\s+)?(\\d+)\\s*Minutes?\\s*\\)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d+)h", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d+)m", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern RARITY_PATTERN = Pattern.compile("\\b(LEGENDARY|MYTHIC)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_INLINE_UNLOCK_PATTERN =
        Pattern.compile("^\\(?LVL\\s*(\\d+)\\)?\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_ENCODED_UNLOCK_PATTERN =
        Pattern.compile("^S\\.([0-9.]+)\\.E\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_UNLOCK_ONLY_PATTERN =
        Pattern.compile("^(?:\\(?LVL\\s*(\\d+)\\)?|S\\.([0-9.]+)\\.E)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_VALUE_STAT_PATTERN =
        Pattern.compile("(.+?)\\s*\\+\\s*([0-9.]+)\\s*%?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_APPENDED_PERCENT_PATTERN = Pattern.compile("\\(\\d+%\\)\\s*$");
    private static final Pattern PET_CURRENT_LEVEL_PATTERN = Pattern.compile("Level:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_EFFECTS_HEADER_PATTERN = Pattern.compile("^Pet Effects:?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FAMILIAR_EFFECTS_HEADER_PATTERN = Pattern.compile("^Familiar Effects:?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINION_EFFECTS_HEADER_PATTERN = Pattern.compile("^Minion Effects:?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_STAT_SPACES = Pattern.compile("\\s+");
    private static final Pattern PET_DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern PET_LEADING_DECORATION = Pattern.compile("^[^A-Za-z0-9]+");
    private static final Pattern POWER_PATTERN = Pattern.compile("Power\\s*\\+?\\s*([0-9.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRENGTH_PATTERN = Pattern.compile("Strength\\s*\\+?\\s*([0-9.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEED_PATTERN = Pattern.compile("Speed\\s*\\+?\\s*([0-9.]+)", Pattern.CASE_INSENSITIVE);
    private static final String PET_HIDDEN_LEGENDARY_MARKER = "伴叹";
    private static final String PET_HIDDEN_MYTHIC_MARKER = "愈潮";

    private static final Map<String, Double> PET_BASES = createPetBaseTable();

    private TooltipParsers() {
    }

    public static Optional<AuctionParseResult> parseAuctionHighlight(List<String> lines, int visibleCount) {
        double selling = 0.0D;
        double average = 0.0D;
        int quantity = Math.max(1, visibleCount);

        for (String line : lines) {
            Matcher sellingMatcher = SELLING_PATTERN.matcher(line);
            if (sellingMatcher.find()) {
                selling = NumberParser.parse(sellingMatcher.group(1), sellingMatcher.group(2));
                continue;
            }

            Matcher averageMatcher = AVERAGE_PATTERN.matcher(line);
            if (averageMatcher.find()) {
                average = NumberParser.parse(averageMatcher.group(1), averageMatcher.group(2));
            }
        }

        if (selling <= 0.0D || average <= 0.0D) {
            return Optional.empty();
        }

        double unitPrice = selling / quantity;
        double delta = (unitPrice - average) / average;
        double intensity = Math.min(1.0D, Math.max(0.0D, Math.abs(delta) / 0.5D));
        return Optional.of(new AuctionParseResult(selling, average, quantity, unitPrice, delta, intensity));
    }

    public static Optional<BossTooltipData> parseBossTooltip(List<String> lines, String fallbackName) {
        Integer x = null;
        Integer y = null;
        Integer z = null;
        int remainingSeconds = -1;
        int cycleSeconds = -1;

        for (String line : lines) {
            Matcher coordsMatcher = COORDS_PATTERN.matcher(line);
            if (coordsMatcher.find()) {
                x = Integer.parseInt(coordsMatcher.group(1));
                y = Integer.parseInt(coordsMatcher.group(2));
                z = Integer.parseInt(coordsMatcher.group(3));
                continue;
            }

            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("respawn") || lower.contains("spawn") || lower.contains("apparition")) {
                Integer cycleMinutes = findFirstInt(CYCLE_MINUTES_PATTERN, line);
                if (cycleMinutes == null || cycleMinutes <= 0) {
                    continue;
                }

                int hours = findFirstInt(HOURS_PATTERN, line, 0);
                int minutes = findFirstInt(MINUTES_PATTERN, line, 0);
                int seconds = findFirstInt(SECONDS_PATTERN, line, 0);
                boolean ready = line.toLowerCase(Locale.ROOT).contains("ready");
                boolean hasExplicitDuration = ready || hours > 0 || minutes > 0 || seconds > 0;

                cycleSeconds = cycleMinutes * 60;
                remainingSeconds = hasExplicitDuration ? (ready ? 0 : hours * 3600 + minutes * 60 + seconds) : cycleSeconds;
            }
        }

        if (x == null || y == null || z == null || remainingSeconds < 0 || cycleSeconds <= 0) {
            return Optional.empty();
        }

        String bossName = lines.isEmpty() ? fallbackName : lines.get(0);
        if (bossName == null || bossName.isBlank()) {
            bossName = fallbackName;
        }
        return Optional.of(new BossTooltipData(bossName, x, y, z, remainingSeconds, cycleSeconds));
    }

    public static List<PetStatLine> parsePetRolls(List<String> lines) {
        return parsePetRolls(lines, null);
    }

    public static List<PetStatLine> parsePetRolls(List<String> lines, Consumer<String> debugLogger) {
        boolean explicitLegendary = false;
        boolean explicitMythic = false;
        boolean hiddenLegendary = false;
        boolean hiddenMythic = false;
        int petCurrentLevel = -1;

        for (String line : lines) {
            Matcher rarityMatcher = RARITY_PATTERN.matcher(line);
            if (rarityMatcher.find()) {
                String detected = rarityMatcher.group(1).toUpperCase(Locale.ROOT);
                if ("MYTHIC".equals(detected)) {
                    explicitMythic = true;
                } else if ("LEGENDARY".equals(detected)) {
                    explicitLegendary = true;
                }
            } else if (line.contains(PET_HIDDEN_MYTHIC_MARKER)) {
                hiddenMythic = true;
            } else if (line.contains(PET_HIDDEN_LEGENDARY_MARKER)) {
                hiddenLegendary = true;
            }

            Matcher levelMatcher = PET_CURRENT_LEVEL_PATTERN.matcher(line);
            if (levelMatcher.find()) {
                petCurrentLevel = Integer.parseInt(levelMatcher.group(1));
            }
        }

        String rarity = explicitMythic ? "MYTHIC"
            : (hiddenMythic ? "MYTHIC" : (explicitLegendary ? "LEGENDARY" : (hiddenLegendary ? "LEGENDARY" : "")));
        if (rarity.isBlank()) {
            return List.of();
        }

        double rarityFactor = "MYTHIC".equals(rarity) ? 1.25D : 1.0D;
        List<PetStatLine> matches = new ArrayList<>();
        Integer pendingUnlockLevel = null;
        boolean inPetEffects = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (PET_EFFECTS_HEADER_PATTERN.matcher(line).matches()
                || FAMILIAR_EFFECTS_HEADER_PATTERN.matcher(line).matches()) {
                inPetEffects = true;
                pendingUnlockLevel = null;
                continue;
            }
            if (MINION_EFFECTS_HEADER_PATTERN.matcher(line).matches()) {
                break;
            }
            if (!inPetEffects) {
                continue;
            }
            if (PET_APPENDED_PERCENT_PATTERN.matcher(line).find()) {
                pendingUnlockLevel = null;
                continue;
            }

            String statCandidate = line;
            Integer inlineUnlock = extractPetUnlockLevel(line);
            if (inlineUnlock != null) {
                statCandidate = stripPetUnlockPrefix(line);
            } else {
                Matcher unlockOnlyMatcher = PET_UNLOCK_ONLY_PATTERN.matcher(line);
                if (unlockOnlyMatcher.find()) {
                    pendingUnlockLevel = parsePetUnlockToken(unlockOnlyMatcher.group(1), unlockOnlyMatcher.group(2));
                    continue;
                }
            }

            int unlockLevel;
            if (inlineUnlock != null) {
                unlockLevel = inlineUnlock;
                pendingUnlockLevel = null;
            } else if (pendingUnlockLevel != null) {
                unlockLevel = pendingUnlockLevel;
                pendingUnlockLevel = null;
            } else {
                continue;
            }

            Matcher valueMatcher = PET_VALUE_STAT_PATTERN.matcher(statCandidate);
            if (valueMatcher.find()) {
                appendPetStatLine(matches, i, rarity, rarityFactor, unlockLevel, petCurrentLevel,
                    valueMatcher.group(1), valueMatcher.group(2), line, debugLogger);
            }
        }

        return matches;
    }

    public static Optional<ProfileStatsData> parseProfileStats(List<String> lines) {
        Double power = null;
        Double strength = null;
        Double speed = null;

        for (String line : lines) {
            Matcher powerMatcher = POWER_PATTERN.matcher(line);
            if (powerMatcher.find()) {
                power = Double.parseDouble(powerMatcher.group(1));
            }

            Matcher strengthMatcher = STRENGTH_PATTERN.matcher(line);
            if (strengthMatcher.find()) {
                strength = Double.parseDouble(strengthMatcher.group(1));
            }

            Matcher speedMatcher = SPEED_PATTERN.matcher(line);
            if (speedMatcher.find()) {
                speed = Double.parseDouble(speedMatcher.group(1));
            }
        }

        if (power == null && strength == null && speed == null) {
            return Optional.empty();
        }

        return Optional.of(new ProfileStatsData(power, strength, speed));
    }

    private static int parseOptionalInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private static Integer findFirstInt(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        return parseOptionalInt(matcher.group(1));
    }

    private static int findFirstInt(Pattern pattern, String line, int fallback) {
        Integer value = findFirstInt(pattern, line);
        return value == null ? fallback : value;
    }

    private static Map<String, Double> createPetBaseTable() {
        Map<String, Double> values = new LinkedHashMap<>();
        putPetBase(values, "Health", 100.0D);
        putPetBase(values, "Energy", 100.0D);
        putPetBase(values, "Power", 20.0D);
        putPetBase(values, "Strength", 10.0D);
        putPetBase(values, "Defence", 10.0D);
        putPetBase(values, "Defense", 10.0D);
        putPetBase(values, "Dexterity", 5.0D);
        putPetBase(values, "Speed", 5.0D);
        putPetBase(values, "Regeneration", 5.0D);
        putPetBase(values, "Energy Regeneration", 5.0D);
        putPetBase(values, "Critical Damage", 5.0D);
        putPetBase(values, "Critical Chance", 2.5D);
        return values;
    }

    private static void putPetBase(Map<String, Double> values, String rawKey, double baseValue) {
        values.put(normalizePetStatKey(rawKey), baseValue);
    }

    private static String normalizePetStatKey(String rawStatName) {
        String stripped = PET_LEADING_DECORATION.matcher(rawStatName == null ? "" : rawStatName.trim()).replaceFirst("");
        String collapsed = PET_STAT_SPACES.matcher(stripped).replaceAll(" ");
        String withoutAccents = PET_DIACRITICS.matcher(Normalizer.normalize(collapsed, Normalizer.Form.NFD)).replaceAll("");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }

    private static void appendPetStatLine(List<PetStatLine> matches, int lineIndex, String rarity, double rarityFactor, int unlockLevel,
                                          int petCurrentLevel, String rawStatName, String rawValue, String rawLine,
                                          Consumer<String> debugLogger) {
        if (unlockLevel <= 0) {
            return;
        }

        String cleanedStatName = rawStatName.trim();
        String key = normalizePetStatKey(cleanedStatName);
        Double baseValue = PET_BASES.get(key);
        if (baseValue == null) {
            return;
        }

        double value = Double.parseDouble(rawValue);
        int scalingLevel = resolvePetScalingLevel(unlockLevel, petCurrentLevel);
        double maxAtLevel = baseValue * rarityFactor * (scalingLevel / 10.0D);
        if (maxAtLevel <= 0.0D) {
            return;
        }

        double percent = Math.max(0.0D, Math.min(100.0D, value / maxAtLevel * 100.0D));
        if (debugLogger != null) {
            debugLogger.accept(String.format(
                Locale.ROOT,
                "[PET] rarity=%s unlock=%d level=%d stat=%s value=%.2f max=%.2f pct=%.2f",
                rarity,
                unlockLevel,
                scalingLevel,
                cleanedStatName,
                value,
                maxAtLevel,
                percent
            ));
        }
        matches.add(new PetStatLine(lineIndex, cleanedStatName, value, percent, rawLine));
    }

    private static int resolvePetScalingLevel(int unlockLevel, int petCurrentLevel) {
        if (petCurrentLevel > 0) {
            return Math.max(unlockLevel, petCurrentLevel);
        }
        return unlockLevel;
    }

    private static Integer extractPetUnlockLevel(String line) {
        Matcher inlineMatcher = PET_INLINE_UNLOCK_PATTERN.matcher(line);
        if (inlineMatcher.find()) {
            return parsePetUnlockToken(inlineMatcher.group(1), null);
        }

        Matcher encodedMatcher = PET_ENCODED_UNLOCK_PATTERN.matcher(line);
        if (encodedMatcher.find()) {
            return parsePetUnlockToken(null, encodedMatcher.group(1));
        }
        return null;
    }

    private static String stripPetUnlockPrefix(String line) {
        Matcher inlineMatcher = PET_INLINE_UNLOCK_PATTERN.matcher(line);
        if (inlineMatcher.find()) {
            return inlineMatcher.group(2);
        }

        Matcher encodedMatcher = PET_ENCODED_UNLOCK_PATTERN.matcher(line);
        if (encodedMatcher.find()) {
            return encodedMatcher.group(2);
        }
        return line;
    }

    private static Integer parsePetUnlockToken(String inlineDigits, String encodedDigits) {
        try {
            if (inlineDigits != null && !inlineDigits.isBlank()) {
                return Integer.parseInt(inlineDigits);
            }
            if (encodedDigits != null && !encodedDigits.isBlank()) {
                return Integer.parseInt(encodedDigits.replace(".", ""));
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    public record AuctionParseResult(double sellingPrice, double averagePrice, int quantity, double unitPrice, double delta,
                                     double intensity) {
    }

    public record BossTooltipData(String bossName, int x, int y, int z, int remainingSeconds, int cycleSeconds) {
    }

    public record PetStatLine(int lineIndex, String statName, double value, double percent, String rawLine) {
    }

    public record ProfileStatsData(Double power, Double strength, Double speed) {
    }
}
