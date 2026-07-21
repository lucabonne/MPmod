package net.minepiece.qol.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class LocalizedText {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern LEADING_DECORATION = Pattern.compile("^[^\\p{L}\\p{N}]+");

    private LocalizedText() {
    }

    public static String normalized(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutAccents = DIACRITICS.matcher(decomposed).replaceAll("");
        return WHITESPACE.matcher(withoutAccents.toLowerCase(Locale.ROOT).trim()).replaceAll(" ");
    }

    public static String normalizedContent(String text) {
        return LEADING_DECORATION.matcher(normalized(text)).replaceFirst("");
    }

    public static boolean containsAny(String text, String... keywords) {
        String normalizedText = normalizedContent(text);
        if (normalizedText.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && normalizedText.contains(normalized(keyword))) {
                return true;
            }
        }
        return false;
    }

    public static boolean startsWithAny(String text, String... prefixes) {
        String normalizedText = normalizedContent(text);
        if (normalizedText.isBlank()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isBlank() && normalizedText.startsWith(normalized(prefix))) {
                return true;
            }
        }
        return false;
    }
}
