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
import net.minepiece.qol.util.LocalizedText;
import net.minepiece.qol.util.NumberParser;

public final class TooltipParsers {
    private static final String NUMBER_TOKEN = "[0-9][0-9., \\u00a0]*";
    private static final String[] AUCTION_SELLING_LABELS = {
        "Selling price",
        "Prix de vente",
        "Precio de venta",
        "Verkaufspreis",
        "Prezzo di vendita",
        "Preço de venda",
        "Cena sprzedaży",
        "Harga jual",
        "Satis fiyati",
        "Satış fiyatı"
    };
    private static final String AUCTION_SELLING_LABEL_PATTERN = String.join("|", AUCTION_SELLING_LABELS);
    private static final Pattern SELLING_PATTERN = Pattern.compile("(?:" + AUCTION_SELLING_LABEL_PATTERN + ")\\s*:?\\s*(" + NUMBER_TOKEN + ")\\s*([KMB])?", Pattern.CASE_INSENSITIVE);
    private static final Pattern AVERAGE_PATTERN = Pattern.compile("(?:Average price|Prix moyen|Precio medio|Durchschnittspreis|Prezzo medio|Preço médio|Srednia cena|Średnia cena|Harga rata-rata|Ortalama fiyat)\\s*:?\\s*(" + NUMBER_TOKEN + ")\\s*([KMB])?", Pattern.CASE_INSENSITIVE);
    private static final Pattern COORDS_PATTERN =
        Pattern.compile("(?:Coord|Coordonnees|Coordonnées|Coordenadas|Koordinaten|Coordinate|Koordynaty|Koordinat)[^0-9-]*(-?\\d+)\\D+(-?\\d+)\\D+(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CYCLE_MINUTES_PATTERN =
        Pattern.compile("\\(?\\s*(?:(?:Every|Toutes les|Cada|Alle|Ogni|Co|Setiap|Her)\\s+)?(\\d+)\\s*(?:Minutes?|Minutos?|Minuten?|Minuti|Minuty|Menit|Dakika)\\s*\\)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CYCLE_SLASH_M_PATTERN =
        Pattern.compile("/\\s*(\\d+)\\s*m\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CYCLE_PAREN_M_PATTERN =
        Pattern.compile("\\((\\d+)\\s*m\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d+)h", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d+)m", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern RARITY_PATTERN = Pattern.compile("\\b(LEGENDARY|MYTHIC|LEGENDAIRE|LÉGENDAIRE|LEGENDARIO|LEGENDARIA|LEGENDARNY|MITICO|MÍTICO|MITYCZNY|MITOS|MITIK|EFSANEVI)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_INLINE_UNLOCK_PATTERN =
        Pattern.compile("^[^\\p{L}\\p{N}]*(?:\\(?\\s*(?:LVL|LV|LEVEL|NIVEL)\\s*(\\d+)\\s*\\)?)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_ENCODED_UNLOCK_PATTERN =
        Pattern.compile("^[^\\p{L}\\p{N}]*S\\.([0-9.]+)\\.E\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_UNLOCK_ONLY_PATTERN =
        Pattern.compile("^[^\\p{L}\\p{N}]*(?:\\(?\\s*(?:LVL|LV|LEVEL|NIVEL)\\s*(\\d+)\\s*\\)?|S\\.([0-9.]+)\\.E)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_VALUE_STAT_PATTERN =
        Pattern.compile("(.+?)\\s*\\+\\s*(" + NUMBER_TOKEN + ")\\s*%?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_APPENDED_PERCENT_PATTERN = Pattern.compile("\\(\\d+%\\)\\s*$");
    private static final Pattern PET_CURRENT_LEVEL_PATTERN = Pattern.compile("(?:Level|Niveau|Nivel|Stufe|Livello|Poziom|Seviye)\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_EFFECTS_HEADER_PATTERN = Pattern.compile("^(?:Pet Effects|Stats|Statistics|Estadisticas|Estadísticas|Effets du familier|Efectos de mascota|Haustier Effekte|Effetti pet|Efeitos do pet|Efekty peta|Efek pet|Evcil hayvan etkileri)\\s*:?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FAMILIAR_EFFECTS_HEADER_PATTERN = Pattern.compile("^(?:Familiar Effects|Effets du familier|Efectos de familiar|Efectos del familiar|Vertrauten Effekte|Effetti famiglio|Efeitos do familiar|Efekty towarzysza|Efek familiar|Yoldas etkileri|Yoldaş etkileri)\\s*:?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINION_EFFECTS_HEADER_PATTERN = Pattern.compile("^(?:Minion Effects|Effets du serviteur|Efectos de esbirro|Efectos de minion|Efectos del minion|Diener Effekte|Effetti servitore|Efeitos do minion|Efekty miniona|Efek minion|Minyon etkileri)\\s*:?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_STAT_SPACES = Pattern.compile("\\s+");
    private static final Pattern PET_DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern PET_LEADING_DECORATION = Pattern.compile("^[^A-Za-z0-9]+");
    private static final Pattern PROFILE_VALUE_PATTERN = Pattern.compile("([+-]?[0-9][0-9.,]*)");
    private static final String PET_HIDDEN_LEGENDARY_MARKER = "伴叹";
    private static final String PET_HIDDEN_MYTHIC_MARKER = "愈潮";
    private static final int DEFAULT_BOSS_CYCLE_SECONDS = 15 * 60;

    private static final Map<String, PetRange> PET_RANGES_LEGENDARY = createLegendaryPetRangeTable();
    private static final Map<String, PetRange> PET_RANGES_MYTHIC = createMythicPetRangeTable();

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

    public static boolean isAuctionSellingPriceLine(String line) {
        return LocalizedText.startsWithAny(line, AUCTION_SELLING_LABELS);
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
            if (isBossTimerLine(line)) {
                int hours = findFirstInt(HOURS_PATTERN, line, 0);
                int minutes = findFirstInt(MINUTES_PATTERN, line, 0);
                int seconds = findFirstInt(SECONDS_PATTERN, line, 0);
                boolean ready = LocalizedText.containsAny(line, "ready", "pret", "prêt", "listo", "bereit", "pronto", "gotowy", "siap", "hazir", "hazır");
                boolean hasExplicitDuration = ready || hours > 0 || minutes > 0 || seconds > 0;
                int explicitRemainingSeconds = ready ? 0 : (hours * 3600 + minutes * 60 + seconds);

                Integer cycleMinutes = findCycleMinutes(line);
                if (cycleMinutes != null && cycleMinutes > 0) {
                    cycleSeconds = cycleMinutes * 60;
                } else if (explicitRemainingSeconds > 0) {
                    cycleSeconds = Math.max(DEFAULT_BOSS_CYCLE_SECONDS, explicitRemainingSeconds);
                } else {
                    cycleSeconds = DEFAULT_BOSS_CYCLE_SECONDS;
                }

                remainingSeconds = hasExplicitDuration ? explicitRemainingSeconds : cycleSeconds;
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

    private static Integer findCycleMinutes(String line) {
        Integer cycle = findFirstInt(CYCLE_MINUTES_PATTERN, line);
        if (cycle != null && cycle > 0) {
            return cycle;
        }
        cycle = findFirstInt(CYCLE_SLASH_M_PATTERN, line);
        if (cycle != null && cycle > 0) {
            return cycle;
        }
        cycle = findFirstInt(CYCLE_PAREN_M_PATTERN, line);
        return (cycle != null && cycle > 0) ? cycle : null;
    }

    public static List<PetStatLine> parsePetRolls(List<String> lines) {
        return parsePetRolls(lines, null);
    }

    public static List<PetStatLine> parsePetRolls(List<String> lines, Consumer<String> debugLogger) {
        return parsePetRolls(lines, debugLogger, "");
    }

    public static List<PetStatLine> parsePetRolls(List<String> lines, Consumer<String> debugLogger, String rarityHint) {
        boolean isMythic = false;
        boolean isLegendary = false;
        int petCurrentLevel = -1;

        for (String line : lines) {
            Matcher rarityMatcher = RARITY_PATTERN.matcher(line);
            if (rarityMatcher.find()) {
                String detected = LocalizedText.normalized(rarityMatcher.group(1));
                isMythic |= detected.contains("mythic") || detected.contains("mitic") || detected.contains("mitycz") || detected.contains("mitos") || detected.contains("mitik");
                isLegendary |= detected.contains("legend");
            } else if (line.contains(PET_HIDDEN_MYTHIC_MARKER)) {
                isMythic = true;
            } else if (line.contains(PET_HIDDEN_LEGENDARY_MARKER)) {
                isLegendary = true;
            }

            Matcher levelMatcher = PET_CURRENT_LEVEL_PATTERN.matcher(line);
            if (levelMatcher.find()) {
                petCurrentLevel = Integer.parseInt(levelMatcher.group(1));
            }
        }

        String rarity = isMythic ? "MYTHIC" : (isLegendary ? "LEGENDARY" : "");
        if (rarity.isEmpty()) {
            String normalizedHint = rarityHint == null ? "" : rarityHint.trim().toUpperCase(Locale.ROOT);
            if ("MYTHIC".equals(normalizedHint) || "LEGENDARY".equals(normalizedHint)) {
                rarity = normalizedHint;
            }
        }
        if (rarity.isEmpty()) {
            return List.of();
        }

        List<PetStatLine> matches = new ArrayList<>();
        Integer pendingUnlockLevel = null;
        boolean inPetEffects = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isPetEffectsHeader(line)) {
                inPetEffects = true;
                pendingUnlockLevel = null;
                continue;
            }
            if (isMinionEffectsHeader(line)) {
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

            Integer unlockLevel = null;
            if (inlineUnlock != null) {
                unlockLevel = inlineUnlock;
                pendingUnlockLevel = null;
            } else if (pendingUnlockLevel != null) {
                unlockLevel = pendingUnlockLevel;
                pendingUnlockLevel = null;
            }

            Matcher valueMatcher = PET_VALUE_STAT_PATTERN.matcher(statCandidate);
            if (valueMatcher.find()) {
                String canonicalStatName = canonicalPetStatName(valueMatcher.group(1));
                if (unlockLevel == null && canonicalStatName.isBlank()) {
                    continue;
                }
                if (unlockLevel == null && petCurrentLevel <= 0) {
                    continue;
                }
                if (unlockLevel == null) {
                    unlockLevel = petCurrentLevel;
                }
                appendPetStatLine(matches, i, rarity, unlockLevel, petCurrentLevel,
                    valueMatcher.group(1), valueMatcher.group(2), line, debugLogger);
            }
        }

        return matches;
    }

    public static Optional<ProfileStatsData> parseProfileStats(List<String> lines) {
        Double level = null;
        Double health = null;
        Double strength = null;
        Double damage = null;
        Double criticalChance = null;
        Double criticalDamage = null;
        Double power = null;
        Double energy = null;
        Double energyRegeneration = null;
        Double speed = null;
        Double dexterity = null;
        Double defense = null;
        Double regeneration = null;

        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (level == null) level = parseProfileValueAfterLabels(line, "Level", "Niveau", "Nivel", "Stufe", "Livello", "Poziom", "Seviye");
            if (health == null) health = parseProfileValueAfterLabels(line, "Health", "Sante", "Santé", "Salud", "Leben", "Salute", "Vida", "Zdrowie", "Kesehatan", "Can");
            if (strength == null) strength = parseProfileValueAfterLabels(line, "Strength", "Force", "Fuerza", "Starke", "Stärke", "Forza", "Sila", "Siła", "Kekuatan", "Guc", "Güç");
            if (criticalChance == null) criticalChance = parseProfileValueAfterLabels(line, "Critical Chance", "Chance critique", "Probabilidad critica", "Probabilidad crítica", "Kritische Chance", "Probabilita critica", "Probabilità critica", "Szansa krytyczna", "Peluang kritis", "Kritik sans", "Kritik şans");
            if (criticalDamage == null) criticalDamage = parseProfileValueAfterLabels(line, "Critical Damage", "Degats critiques", "Dégâts critiques", "Dano critico", "Daño crítico", "Kritischer Schaden", "Danno critico", "Obrazenia krytyczne", "Obrażenia krytyczne", "Kerusakan kritis", "Kritik hasar");
            if (damage == null && !containsAnyLabel(lower, "critical damage", "degats critiques", "dégâts critiques", "dano critico", "daño crítico", "kritischer schaden", "danno critico", "obrazenia krytyczne", "obrażenia krytyczne", "kerusakan kritis", "kritik hasar")) {
                damage = parseProfileValueAfterLabels(line, "Damage", "Degats", "Dégâts", "Dano", "Daño", "Schaden", "Danno", "Obrazenia", "Obrażenia", "Kerusakan", "Hasar");
            }
            if (power == null) power = parseProfileValueAfterLabels(line, "Power", "Puissance", "Poder", "Kraft", "Potere", "Moc", "Kekuatan", "Guc", "Güç");
            if (energyRegeneration == null) energyRegeneration = parseProfileValueAfterLabels(line, "Energy Regeneration", "Regeneration d'energie", "Régénération d'énergie", "Regeneracion de energia", "Regeneración de energía", "Energieregeneration", "Rigenerazione energia", "Regeneracja energii", "Regenerasi energi", "Enerji yenilenmesi");
            if (energy == null && !containsAnyLabel(lower, "energy regeneration", "regeneration d'energie", "régénération d'énergie", "regeneracion de energia", "regeneración de energía", "energieregeneration", "rigenerazione energia", "regeneracja energii", "regenerasi energi", "enerji yenilenmesi")) {
                energy = parseProfileValueAfterLabels(line, "Energy", "Energie", "Énergie", "Energia", "Energía", "Enerji");
            }
            if (speed == null) speed = parseProfileValueAfterLabels(line, "Speed", "Vitesse", "Velocidad", "Geschwindigkeit", "Velocita", "Velocità", "Predkosc", "Prędkość", "Kecepatan", "Hiz", "Hız");
            if (dexterity == null) dexterity = parseProfileValueAfterLabels(line, "Dexterity", "Dexterite", "Dextérité", "Destreza", "Geschicklichkeit", "Destrezza", "Zrecznosc", "Zręczność", "Ketangkasan", "Ceviklik");
            if (defense == null) {
                defense = parseProfileValueAfterLabels(line, "Defense", "Defence", "Defense", "Défense", "Defensa", "Verteidigung", "Difesa", "Obrona", "Pertahanan", "Savunma");
                if (defense == null) {
                    defense = parseProfileValueAfterLabel(line, "Defence");
                }
            }
            if (regeneration == null) regeneration = parseProfileValueAfterLabels(line, "Regeneration", "Regeneration de vie", "Régénération de vie", "Regeneracion", "Regeneración", "Regenerierung", "Rigenerazione", "Regeneracja", "Regenerasi", "Yenilenme");
        }

        if (level == null
            && health == null
            && strength == null
            && damage == null
            && criticalChance == null
            && criticalDamage == null
            && power == null
            && energy == null
            && energyRegeneration == null
            && speed == null
            && dexterity == null
            && defense == null
            && regeneration == null) {
            return Optional.empty();
        }

        return Optional.of(new ProfileStatsData(
            level,
            health,
            strength,
            damage,
            criticalChance,
            criticalDamage,
            power,
            energy,
            energyRegeneration,
            speed,
            dexterity,
            defense,
            regeneration
        ));
    }

    private static Double parseProfileValueAfterLabel(String line, String label) {
        if (line == null || label == null || label.isBlank()) {
            return null;
        }
        String lowerLine = line.toLowerCase(Locale.ROOT);
        String lowerLabel = label.toLowerCase(Locale.ROOT);
        int labelIndex = lowerLine.indexOf(lowerLabel);
        if (labelIndex < 0) {
            return null;
        }
        int tailStart = Math.min(line.length(), labelIndex + label.length());
        Matcher matcher = PROFILE_VALUE_PATTERN.matcher(line.substring(tailStart));
        if (!matcher.find()) {
            return null;
        }
        String token = matcher.group(1);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(token.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer findFirstInt(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private static int findFirstInt(Pattern pattern, String line, int fallback) {
        Integer value = findFirstInt(pattern, line);
        return value == null ? fallback : value;
    }

    private static Map<String, PetRange> createLegendaryPetRangeTable() {
        Map<String, PetRange> ranges = new LinkedHashMap<>();
        putPetRange(ranges, "Strength", 5.0D, 10.0D);
        putPetRange(ranges, "Fuerza", 5.0D, 10.0D);
        putPetRange(ranges, "Power", 10.0D, 20.0D);
        putPetRange(ranges, "Poder", 10.0D, 20.0D);
        putPetRange(ranges, "Critical Chance", 1.25D, 2.5D);
        putPetRange(ranges, "Probabilidad Critica", 1.25D, 2.5D);
        putPetRange(ranges, "Probabilidad Crítica", 1.25D, 2.5D);
        putPetRange(ranges, "Prob. Critico", 1.25D, 2.5D);
        putPetRange(ranges, "Prob. Crítico", 1.25D, 2.5D);
        putPetRange(ranges, "Critical Damage", 2.5D, 5.0D);
        putPetRange(ranges, "Dano Critico", 2.5D, 5.0D);
        putPetRange(ranges, "Daño Crítico", 2.5D, 5.0D);
        putPetRange(ranges, "Damage", 2.5D, 5.0D);
        putPetRange(ranges, "Dano", 2.5D, 5.0D);
        putPetRange(ranges, "Daño", 2.5D, 5.0D);
        putPetRange(ranges, "Defense", 5.0D, 10.0D);
        putPetRange(ranges, "Defence", 5.0D, 10.0D);
        putPetRange(ranges, "Defensa", 5.0D, 10.0D);
        putPetRange(ranges, "Speed", 2.5D, 5.0D);
        putPetRange(ranges, "Velocidad", 2.5D, 5.0D);
        putPetRange(ranges, "Regeneration", 2.5D, 5.0D);
        putPetRange(ranges, "Life Regeneration", 2.5D, 5.0D);
        putPetRange(ranges, "Regeneracion", 2.5D, 5.0D);
        putPetRange(ranges, "Regeneración", 2.5D, 5.0D);
        putPetRange(ranges, "HP", 50.0D, 100.0D);
        putPetRange(ranges, "Health", 50.0D, 100.0D);
        putPetRange(ranges, "Vida", 50.0D, 100.0D);
        putPetRange(ranges, "Energy", 50.0D, 100.0D);
        putPetRange(ranges, "Energia", 50.0D, 100.0D);
        putPetRange(ranges, "Energía", 50.0D, 100.0D);
        putPetRange(ranges, "Energy Regeneration", 2.5D, 5.0D);
        putPetRange(ranges, "Regeneracion de Energia", 2.5D, 5.0D);
        putPetRange(ranges, "Regeneración de Energía", 2.5D, 5.0D);
        putPetRange(ranges, "Regen. de Energia", 2.5D, 5.0D);
        putPetRange(ranges, "Regen. de Energía", 2.5D, 5.0D);
        putPetRange(ranges, "Dexterity", 2.5D, 5.0D);
        putPetRange(ranges, "Destreza", 2.5D, 5.0D);
        return ranges;
    }

    private static Map<String, PetRange> createMythicPetRangeTable() {
        Map<String, PetRange> ranges = new LinkedHashMap<>();
        putPetRange(ranges, "Strength", 7.5D, 12.5D);
        putPetRange(ranges, "Fuerza", 7.5D, 12.5D);
        putPetRange(ranges, "Power", 15.0D, 25.0D);
        putPetRange(ranges, "Poder", 15.0D, 25.0D);
        putPetRange(ranges, "Critical Chance", 1.875D, 3.125D);
        putPetRange(ranges, "Probabilidad Critica", 1.875D, 3.125D);
        putPetRange(ranges, "Probabilidad Crítica", 1.875D, 3.125D);
        putPetRange(ranges, "Prob. Critico", 1.875D, 3.125D);
        putPetRange(ranges, "Prob. Crítico", 1.875D, 3.125D);
        putPetRange(ranges, "Critical Damage", 3.75D, 6.25D);
        putPetRange(ranges, "Dano Critico", 3.75D, 6.25D);
        putPetRange(ranges, "Daño Crítico", 3.75D, 6.25D);
        putPetRange(ranges, "Damage", 3.75D, 6.25D);
        putPetRange(ranges, "Dano", 3.75D, 6.25D);
        putPetRange(ranges, "Daño", 3.75D, 6.25D);
        putPetRange(ranges, "Energy", 75.0D, 150.0D);
        putPetRange(ranges, "Energia", 75.0D, 150.0D);
        putPetRange(ranges, "Energía", 75.0D, 150.0D);
        putPetRange(ranges, "Energy Regeneration", 3.75D, 6.25D);
        putPetRange(ranges, "Regeneracion de Energia", 3.75D, 6.25D);
        putPetRange(ranges, "Regeneración de Energía", 3.75D, 6.25D);
        putPetRange(ranges, "Regen. de Energia", 3.75D, 6.25D);
        putPetRange(ranges, "Regen. de Energía", 3.75D, 6.25D);
        putPetRange(ranges, "HP", 75.0D, 150.0D);
        putPetRange(ranges, "Health", 75.0D, 150.0D);
        putPetRange(ranges, "Vida", 75.0D, 150.0D);
        putPetRange(ranges, "Regeneration", 3.75D, 6.25D);
        putPetRange(ranges, "Life Regeneration", 3.75D, 6.25D);
        putPetRange(ranges, "Regeneracion", 3.75D, 6.25D);
        putPetRange(ranges, "Regeneración", 3.75D, 6.25D);
        putPetRange(ranges, "Dexterity", 3.75D, 6.25D);
        putPetRange(ranges, "Destreza", 3.75D, 6.25D);
        putPetRange(ranges, "Speed", 3.75D, 6.25D);
        putPetRange(ranges, "Velocidad", 3.75D, 6.25D);
        putPetRange(ranges, "Defense", 7.5D, 12.5D);
        putPetRange(ranges, "Defence", 7.5D, 12.5D);
        putPetRange(ranges, "Defensa", 7.5D, 12.5D);
        return ranges;
    }

    private static void putPetRange(Map<String, PetRange> values, String rawKey, double min, double max) {
        values.put(normalizePetStatKey(rawKey), new PetRange(min, max));
    }

    private static String normalizePetStatKey(String rawStatName) {
        String stripped = PET_LEADING_DECORATION.matcher(rawStatName == null ? "" : rawStatName.trim()).replaceFirst("");
        String collapsed = PET_STAT_SPACES.matcher(stripped).replaceAll(" ");
        String withoutAccents = PET_DIACRITICS.matcher(Normalizer.normalize(collapsed, Normalizer.Form.NFD)).replaceAll("");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }

    public static String canonicalPetStatName(String rawStatName) {
        String key = normalizePetStatKey(rawStatName)
            .replaceAll("[^\\p{L}\\p{N}]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
        if (isPetEffectStatLabel(key)) {
            return "";
        }
        String direct = switch (key) {
            case "strength", "force", "fuerza", "starke", "forza", "sila", "kekuatan", "guc" -> "Strength";
            case "power", "puissance", "poder", "kraft", "potere", "moc" -> "Power";
            case "critical chance", "chance critique", "probabilidad critica", "prob critico", "kritische chance",
                "probabilita critica", "szansa krytyczna", "peluang kritis", "kritik sans" -> "Critical Chance";
            case "critical damage", "degats critiques", "dano critico", "kritischer schaden", "danno critico",
                "obrazenia krytyczne", "kerusakan kritis", "kritik hasar" -> "Critical Damage";
            case "damage", "degats", "dano", "schaden", "danno", "obrazenia", "kerusakan", "hasar" -> "Damage";
            case "defense", "defence", "defensa", "verteidigung", "difesa", "obrona", "pertahanan", "savunma" -> "Defense";
            case "speed", "vitesse", "velocidad", "geschwindigkeit", "velocita", "predkosc", "kecepatan", "hiz" -> "Speed";
            case "regeneration", "life regeneration", "regeneracion", "regenerierung", "rigenerazione", "regeneracja",
                "regenerasi", "yenilenme" -> "Regeneration";
            case "hp", "health", "sante", "salud", "leben", "salute", "vida", "zdrowie", "kesehatan", "can" -> "Health";
            case "energy", "energie", "energia", "enerji" -> "Energy";
            case "energy regeneration", "regeneration d energie", "regeneracion de energia", "regen de energia",
                "energieregeneration", "rigenerazione energia", "regeneracja energii", "regenerasi energi",
                "enerji yenilenmesi" -> "Energy Regeneration";
            case "dexterity", "dexterite", "destreza", "geschicklichkeit", "destrezza", "zrecznosc", "ketangkasan",
                "ceviklik" -> "Dexterity";
            default -> "";
        };
        if (!direct.isBlank()) {
            return direct;
        }

        if (key.startsWith("critical chance") || key.startsWith("chance critique") || key.startsWith("probabilidad critica")
            || key.startsWith("prob critico") || key.startsWith("kritische chance") || key.startsWith("probabilita critica")
            || key.startsWith("szansa krytyczna") || key.startsWith("peluang kritis") || key.startsWith("kritik sans")) {
            return "Critical Chance";
        }
        if (key.startsWith("critical damage") || key.startsWith("degats critiques") || key.startsWith("dano critico")
            || key.startsWith("kritischer schaden") || key.startsWith("danno critico") || key.startsWith("obrazenia krytyczne")
            || key.startsWith("kerusakan kritis") || key.startsWith("kritik hasar")) {
            return "Critical Damage";
        }
        if (key.startsWith("energy regeneration") || key.startsWith("regeneration d energie")
            || key.startsWith("regeneracion de energia") || key.startsWith("regen de energia")
            || key.startsWith("energieregeneration") || key.startsWith("rigenerazione energia")
            || key.startsWith("regeneracja energii") || key.startsWith("regenerasi energi")
            || key.startsWith("enerji yenilenmesi")) {
            return "Energy Regeneration";
        }
        if (key.startsWith("regeneration") || key.startsWith("life regeneration") || key.startsWith("regeneracion")
            || key.startsWith("regenerierung") || key.startsWith("rigenerazione") || key.startsWith("regeneracja")
            || key.startsWith("regenerasi") || key.startsWith("yenilenme")) {
            return "Regeneration";
        }
        if (key.startsWith("strength") || key.startsWith("force") || key.startsWith("fuerza") || key.startsWith("starke")
            || key.startsWith("forza") || key.startsWith("sila") || key.startsWith("kekuatan") || key.startsWith("guc")) {
            return "Strength";
        }
        if (key.startsWith("power") || key.startsWith("puissance") || key.startsWith("poder") || key.startsWith("kraft")
            || key.startsWith("potere") || key.startsWith("moc")) {
            return "Power";
        }
        if (key.startsWith("damage") || key.startsWith("degats") || key.startsWith("dano") || key.startsWith("schaden")
            || key.startsWith("danno") || key.startsWith("obrazenia") || key.startsWith("kerusakan") || key.startsWith("hasar")) {
            return "Damage";
        }
        if (key.startsWith("defense") || key.startsWith("defence") || key.startsWith("defensa") || key.startsWith("verteidigung")
            || key.startsWith("difesa") || key.startsWith("obrona") || key.startsWith("pertahanan") || key.startsWith("savunma")) {
            return "Defense";
        }
        if (key.startsWith("speed") || key.startsWith("vitesse") || key.startsWith("velocidad") || key.startsWith("geschwindigkeit")
            || key.startsWith("velocita") || key.startsWith("predkosc") || key.startsWith("kecepatan") || key.startsWith("hiz")) {
            return "Speed";
        }
        if (key.startsWith("hp") || key.startsWith("health") || key.startsWith("sante") || key.startsWith("salud") || key.startsWith("leben")
            || key.startsWith("salute") || key.startsWith("vida") || key.startsWith("zdrowie") || key.startsWith("kesehatan")
            || key.startsWith("can")) {
            return "Health";
        }
        if (key.startsWith("energy") || key.startsWith("energie") || key.startsWith("energia") || key.startsWith("enerji")) {
            return "Energy";
        }
        if (key.startsWith("dexterity") || key.startsWith("dexterite") || key.startsWith("destreza")
            || key.startsWith("geschicklichkeit") || key.startsWith("destrezza") || key.startsWith("zrecznosc")
            || key.startsWith("ketangkasan") || key.startsWith("ceviklik")) {
            return "Dexterity";
        }
        return "";
    }

    public static boolean isPetEffectStatLabel(String rawStatName) {
        String key = normalizePetStatKey(rawStatName)
            .replaceAll("[^\\p{L}\\p{N}]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
        return key.startsWith("dano de ")
            || key.startsWith("damage of ")
            || key.startsWith("degats de ")
            || key.startsWith("degats du ")
            || key.startsWith("danno di ")
            || key.startsWith("schaden von ");
    }

    private static boolean isPetEffectsHeader(String line) {
        return PET_EFFECTS_HEADER_PATTERN.matcher(line).matches()
            || FAMILIAR_EFFECTS_HEADER_PATTERN.matcher(line).matches()
            || LocalizedText.startsWithAny(line,
                "pet effects", "familiar effects", "stats", "statistics", "estadisticas", "estadísticas",
                "effets du familier", "efectos de mascota", "efectos de familiar", "efectos del familiar",
                "haustier effekte", "vertrauten effekte", "effetti pet", "effetti famiglio", "efeitos do pet",
                "efeitos do familiar", "efekty peta", "efekty towarzysza", "efek pet", "efek familiar",
                "evcil hayvan etkileri", "yoldas etkileri", "yoldaş etkileri");
    }

    private static boolean isMinionEffectsHeader(String line) {
        return MINION_EFFECTS_HEADER_PATTERN.matcher(line).matches()
            || LocalizedText.startsWithAny(line,
                "minion effects", "effets du serviteur", "efectos de esbirro", "efectos de minion", "efectos del minion", "diener effekte",
                "effetti servitore", "efeitos do minion", "efekty miniona", "efek minion", "minyon etkileri");
    }

    private static void appendPetStatLine(List<PetStatLine> matches, int lineIndex, String rarity, int unlockLevel,
                                          int petCurrentLevel, String rawStatName, String rawValue, String rawLine,
                                          Consumer<String> debugLogger) {
        if (unlockLevel <= 0) {
            return;
        }

        String cleanedStatName = rawStatName.trim();
        String canonicalStatName = canonicalPetStatName(cleanedStatName);
        String key = normalizePetStatKey(canonicalStatName.isBlank() ? cleanedStatName : canonicalStatName);
        Map<String, PetRange> ranges = "MYTHIC".equals(rarity) ? PET_RANGES_MYTHIC : PET_RANGES_LEGENDARY;
        PetRange baseRange = ranges.get(key);
        if (baseRange == null) {
            return;
        }

        double value = NumberParser.parse(rawValue, null);
        int scalingLevel = resolvePetScalingLevel(unlockLevel, petCurrentLevel);
        double scale = scalingLevel / 10.0D;
        double minAtLevel = baseRange.min() * scale;
        double maxAtLevel = baseRange.max() * scale;
        if (maxAtLevel <= minAtLevel) {
            return;
        }

        double percent = Math.max(0.0D, Math.min(100.0D, (value - minAtLevel) / (maxAtLevel - minAtLevel) * 100.0D));
        if (debugLogger != null) {
            debugLogger.accept(String.format(
                Locale.ROOT,
                "[PET] rarity=%s unlock=%d level=%d stat=%s value=%.2f min=%.2f max=%.2f pct=%.2f",
                rarity,
                unlockLevel,
                scalingLevel,
                canonicalStatName.isBlank() ? cleanedStatName : canonicalStatName,
                value,
                minAtLevel,
                maxAtLevel,
                percent
            ));
        }
        matches.add(new PetStatLine(lineIndex, canonicalStatName.isBlank() ? cleanedStatName : canonicalStatName, value, percent, rawLine));
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

    private static Double parseProfileValueAfterLabels(String line, String... labels) {
        for (String label : labels) {
            Double parsed = parseProfileValueAfterLabel(line, label);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static boolean containsAnyLabel(String text, String... labels) {
        return LocalizedText.containsAny(text, labels);
    }

    public static boolean isBossTimerLine(String line) {
        return LocalizedText.containsAny(line,
            "respawn", "spawn", "apparition", "reapparition", "réapparition", "aparicion", "aparición", "reaparicion", "reaparición",
            "erscheint", "wiederbelebung", "rigenerazione", "ricomparsa", "renasce", "pojawia", "odrodzenie", "muncul", "respawn", "yeniden dogma", "yeniden doğma"
        );
    }

    public static boolean isBossInfoLine(String line) {
        return isBossTimerLine(line) || LocalizedText.containsAny(line,
            "coord", "coordonnees", "coordonnées", "coordenadas", "koordinaten", "coordinate", "koordynaty", "koordinat"
        );
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

    public record ProfileStatsData(
        Double level,
        Double health,
        Double strength,
        Double damage,
        Double criticalChance,
        Double criticalDamage,
        Double power,
        Double energy,
        Double energyRegeneration,
        Double speed,
        Double dexterity,
        Double defense,
        Double regeneration
    ) {
    }

    private record PetRange(double min, double max) {
    }
}
