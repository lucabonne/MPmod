package net.minepiece.qol.essentials;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the minion feeding-screen line, e.g. {@code "Andesite x64 - 128.0 Exp"},
 * which reveals how much XP one unit of a resource is worth.
 */
public final class MinionFeedLine {

    public record Feed(String resourceName, int count, double totalExp) {
        public double xpPerItem() {
            return count == 0 ? 0.0 : totalExp / count;
        }
    }

    private static final Pattern LINE =
        Pattern.compile("^(.+?) x(\\d+) - ([\\d.,]+) (?:Exp|XP)$");

    private MinionFeedLine() {}

    public static Optional<Feed> parse(String line) {
        if (line == null) return Optional.empty();
        Matcher m = LINE.matcher(line.trim());
        if (!m.matches()) return Optional.empty();
        try {
        Feed feed = new Feed(
            m.group(1).trim(),
            Integer.parseInt(m.group(2)),
            Double.parseDouble(m.group(3).replace(',', '.')));
        return feed.count() > 0 && Double.isFinite(feed.totalExp()) && feed.totalExp() > 0 ? Optional.of(feed) : Optional.empty();
        } catch (NumberFormatException ignored) { return Optional.empty(); }
    }
}
