package net.minepiece.qol.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Locale;

public final class NumberParser {
    private NumberParser() {
    }

    public static double parse(String value, String suffix) {
        if (value == null || value.isBlank()) {
            return 0.0D;
        }

        String sanitized = sanitize(value);
        double base = Double.parseDouble(sanitized);
        double multiplier = switch (suffix == null ? "" : suffix.trim().toUpperCase(Locale.ROOT)) {
            case "K" -> 1_000D;
            case "M" -> 1_000_000D;
            case "B" -> 1_000_000_000D;
            default -> 1D;
        };
        return base * multiplier;
    }

    public static long parseLong(String value) {
        return Math.round(parse(value, null));
    }

    public static String formatSmartSeconds(long remainingMillis) {
        double seconds = Math.max(0.0D, remainingMillis / 1000.0D);
        if (seconds < 10.0D) {
            return BigDecimal.valueOf(seconds).setScale(1, RoundingMode.DOWN).toPlainString() + "s";
        }
        return Math.round(seconds) + "s";
    }

    public static String formatTimer(long remainingMillis) {
        Duration duration = Duration.ofMillis(Math.max(0L, remainingMillis));
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private static String sanitize(String input) {
        String trimmed = input.trim();
        boolean hasComma = trimmed.contains(",");
        boolean hasDot = trimmed.contains(".");
        if (hasComma && hasDot) {
            return trimmed.replace(",", "");
        }
        if (hasComma) {
            int lastComma = trimmed.lastIndexOf(',');
            int digitsAfterComma = trimmed.length() - lastComma - 1;
            if (digitsAfterComma == 3) {
                return trimmed.replace(",", "");
            }
            return trimmed.replace(',', '.');
        }
        return trimmed;
    }
}
