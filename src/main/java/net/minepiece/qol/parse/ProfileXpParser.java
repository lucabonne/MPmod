package net.minepiece.qol.parse;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.util.NumberParser;

public final class ProfileXpParser {
    private static final Pattern LEVEL = Pattern.compile(
        "\\b(?:level|lvl|lv|niveau|nivel|nível|stufe|livello|poziom|tingkat|seviye)\\s*[:.]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERCENT = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*%");
    private static final Pattern NUMBER = Pattern.compile("[0-9][0-9.,]*");
    private static final Pattern ASCENSION = Pattern.compile(
        "\\b(?:ascension|ascenso|aufstieg|ascesa|ascensao|ascensão|wzniesienie|kenaikan|yükseliş)\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern XP_LABEL = Pattern.compile(
        "\\b(?:xp|exp|experience|expérience|experiencia|erfahrung|esperienza|experiência|doświadczenie|pengalaman|deneyim)\\b", Pattern.CASE_INSENSITIVE);

    private ProfileXpParser() {
    }

    public static Optional<Snapshot> parseTooltip(List<String> lines) {
        int ascension = 0;
        BarProgress levelLine = null;
        for (String line : lines) {
            Matcher ascensionMatcher = ASCENSION.matcher(line);
            if (ascensionMatcher.find()) {
                ascension = Integer.parseInt(ascensionMatcher.group(1));
            }
            Optional<BarProgress> progress = parseBar(line);
            if (progress.isPresent() && progress.get().level() != null) {
                levelLine = progress.get();
                continue;
            }
            if (levelLine == null) {
                continue;
            }
            // The profile tooltip has two numbers separated by a resource-pack progress bar.
            Matcher numbers = NUMBER.matcher(line);
            if (!numbers.find()) {
                continue;
            }
            double current = NumberParser.parse(numbers.group(), null);
            if (!numbers.find()) {
                continue;
            }
            double required = NumberParser.parse(numbers.group(), null);
            if (!numbers.find() && required > 0 && current >= 0 && current <= required
                && Math.abs(current / required * 100.0D - levelLine.percent()) <= 0.011D) {
                return Optional.of(new Snapshot(ascension, levelLine.level(), current, required));
            }
        }
        return Optional.empty();
    }

    public static Optional<BarProgress> parseBar(String text) {
        Matcher percent = PERCENT.matcher(text);
        if (!percent.find()) {
            return Optional.empty();
        }
        double value = Double.parseDouble(percent.group(1).replace(',', '.'));
        if (value > 100.0D || percent.find()) {
            return Optional.empty();
        }
        Matcher level = LEVEL.matcher(text);
        Integer number = level.find() ? Integer.valueOf(level.group(1)) : null;
        return Optional.of(new BarProgress(number, value, XP_LABEL.matcher(text).find()));
    }

    public record Snapshot(int ascension, int level, double currentXp, double requiredXp) {
        public double percent() {
            return this.currentXp / this.requiredXp * 100.0D;
        }
    }

    public record BarProgress(Integer level, double percent, boolean xpLabel) {
    }
}
