package net.minepiece.qol.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.util.NumberParser;

public final class ActionbarParser {
    private static final String NUMBER_TOKEN = "[0-9][0-9., \\u00a0]*";
    private static final Pattern GAIN_PATTERN = Pattern.compile("\\+\\s*(" + NUMBER_TOKEN + ")");
    private static final Pattern SYMBOL_GAIN_PATTERN = Pattern.compile("\\+\\s*(" + NUMBER_TOKEN + ")\\s*([^\\s+])");
    private static final Pattern XP_PATTERN = Pattern.compile("(" + NUMBER_TOKEN + ")\\s*/\\s*(" + NUMBER_TOKEN + ")");

    private ActionbarParser() {
    }

    public static Optional<ActionbarSnapshot> parseJobActionbar(String actionbar) {
        Matcher gainMatcher = GAIN_PATTERN.matcher(actionbar);
        Double moneyGain = null;
        Double xpGain = null;

        if (gainMatcher.find()) {
            moneyGain = NumberParser.parse(gainMatcher.group(1), null);
        }
        if (gainMatcher.find()) {
            xpGain = NumberParser.parse(gainMatcher.group(1), null);
        }

        Matcher xpMatcher = XP_PATTERN.matcher(actionbar);
        if (!xpMatcher.find()) {
            return Optional.empty();
        }

        double currentXp = NumberParser.parse(xpMatcher.group(1), null);
        double neededXp = NumberParser.parse(xpMatcher.group(2), null);
        return Optional.of(new ActionbarSnapshot(moneyGain == null ? 0.0D : moneyGain, xpGain == null ? 0.0D : xpGain, currentXp, neededXp));
    }

    public static List<SymbolGain> parseSymbolGains(String actionbar) {
        Matcher matcher = SYMBOL_GAIN_PATTERN.matcher(actionbar);
        List<SymbolGain> gains = new ArrayList<>();
        while (matcher.find()) {
            try {
                gains.add(new SymbolGain(matcher.group(2), NumberParser.parse(matcher.group(1), null)));
            } catch (RuntimeException ignored) {
            }
        }
        return gains;
    }

    public record ActionbarSnapshot(double moneyGain, double xpGain, double currentXp, double neededXp) {
    }

    public record SymbolGain(String symbol, double value) {
    }
}
