package net.minepiece.qol.essentials;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a rendered "Familier Effects" stat line, e.g.
 * {@code "S.1.5.E 犹 Vie + 105.65"}, into a {@link PetEffect}.
 *
 * <p>The leading token encodes the unlock tier as its digits
 * ({@code S.1.5.E} → 15, {@code S.2.0.E} → 20, {@code S.1.E} → 1). The stat name
 * and value sit around the {@code +}. Lines without a {@code +}, an unknown
 * stat, or no tier digits (headers, money, "Non découvert") yield empty.
 */
public final class PetEffectParser {

    private static final Pattern VALUE = Pattern.compile("\\+\\s*(-?\\d+(?:[.,]\\d+)?)");

    private static final Pattern WS = Pattern.compile("\\s+");

    private PetEffectParser() {}

    public static Optional<PetEffect> parse(String line) {
        if (line == null) return Optional.empty();

        try {
        int plus = line.indexOf('+');
        if (plus < 0) return Optional.empty();

        String[] tokens = WS.split(line.trim());
        if (tokens.length == 0) return Optional.empty();
        int tier = digitsOf(tokens[0]);
        if (tier <= 0) return Optional.empty();

        String statLabel = line.substring(0, plus).replaceFirst("^\\S+\\s+", "");
        String canonical = net.minepiece.qol.parse.TooltipParsers.canonicalPetStatName(statLabel);
        PetStat stat = PetStat.fromLabel(canonical.isBlank() ? statLabel : canonical);
        if (stat == null) return Optional.empty();

        Matcher m = VALUE.matcher(line);
        if (!m.find()) return Optional.empty();
        double value = Double.parseDouble(m.group(1).replace(',', '.'));

        String pre = line.substring(0, plus).trim();
        String[] toks = WS.split(pre);
        StringBuilder lbl = new StringBuilder();
        for (int i = 1; i < toks.length; i++) {
            String t = toks[i];
            boolean hasLatin = t.chars().anyMatch(c ->
                (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= 0x00C0 && c <= 0x024F));
            if (hasLatin) { if (lbl.length() > 0) lbl.append(' '); lbl.append(t); }
        }
        String label = lbl.toString();

        return Double.isFinite(value) ? Optional.of(new PetEffect(tier, stat, value, label)) : Optional.empty();
        } catch (NumberFormatException ignored) { return Optional.empty(); }
    }

    /** Concatenates the digits in a token: {@code "S.1.5.E"} → 15, {@code "S.1.E"} → 1. */
    private static int digitsOf(String token) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.isEmpty() ? 0 : Integer.parseInt(sb.toString());
    }
}
