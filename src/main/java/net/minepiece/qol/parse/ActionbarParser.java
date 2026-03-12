package net.minepiece.qol.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ActionbarParser {
    private static final Pattern GAIN_PATTERN = Pattern.compile("\\+([0-9.]+)");
    private static final Pattern SYMBOL_GAIN_PATTERN = Pattern.compile("\\+([0-9.]+)\\s*([^\\s+])");
    private static final Pattern XP_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)");

    private ActionbarParser() {
    }

    public static Optional<ActionbarSnapshot> parseJobActionbar(String actionbar) {
        Matcher gainMatcher = GAIN_PATTERN.matcher(actionbar);
        Double moneyGain = null;
        Double xpGain = null;

        if (gainMatcher.find()) {
            moneyGain = Double.parseDouble(gainMatcher.group(1));
        }
        if (gainMatcher.find()) {
            xpGain = Double.parseDouble(gainMatcher.group(1));
        }

        Matcher xpMatcher = XP_PATTERN.matcher(actionbar);
        if (!xpMatcher.find()) {
            return Optional.empty();
        }

        double currentXp = Double.parseDouble(xpMatcher.group(1));
        double neededXp = Double.parseDouble(xpMatcher.group(2));
        return Optional.of(new ActionbarSnapshot(moneyGain == null ? 0.0D : moneyGain, xpGain == null ? 0.0D : xpGain, currentXp, neededXp));
    }

    public static List<SymbolGain> parseSymbolGains(String actionbar) {
        Matcher matcher = SYMBOL_GAIN_PATTERN.matcher(actionbar);
        List<SymbolGain> gains = new ArrayList<>();
        while (matcher.find()) {
            try {
                gains.add(new SymbolGain(matcher.group(2), Double.parseDouble(matcher.group(1))));
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
