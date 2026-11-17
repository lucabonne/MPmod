package net.minepiece.qol.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;

public final class TextUtil {
    private static final Pattern LEGACY_FORMATTING = Pattern.compile("\u00a7.");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TextUtil() {
    }

    public static String normalize(Component text) {
        return text == null ? "" : normalize(text.getString());
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }

        String noFormatting = LEGACY_FORMATTING.matcher(raw).replaceAll("");
        return WHITESPACE.matcher(noFormatting.trim()).replaceAll(" ");
    }

    public static List<String> normalizeLines(List<Component> lines) {
        List<String> normalized = new ArrayList<>(lines.size());
        for (Component line : lines) {
            normalized.add(normalize(line));
        }
        return normalized;
    }
}
