package net.minepiece.qol.parse;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.util.NumberParser;

public final class ChatParsers {
    private static final Pattern SPAWN_ID_PATTERN = Pattern.compile("\\(spawn-([a-zA-Z0-9]+)\\)");
    private static final Pattern BOSS_KILL_LEADING_PATTERN = Pattern.compile("[-=]+\\s*Leaderboard\\s+(.+?)\\s*[-=]+");
    private static final Pattern BOSS_KILL_TRAILING_PATTERN = Pattern.compile("[-=]+\\s*(.+?)\\s+Leaderboard\\s*[-=]+");
    private static final Pattern SELL_PATTERN = Pattern.compile("Votre objet\\s+(.+?)\\s+a été vendu pour\\s+([0-9.,]+)\\s*([KMB])?",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern BUY_PATTERN = Pattern.compile(
        "Vous avez acheté avec succès\\s+(.+?)\\s+\\(x\\d+\\)\\s+pour\\s+([0-9.,]+)\\s*([KMB])?",
        Pattern.CASE_INSENSITIVE);

    private ChatParsers() {
    }

    public static Optional<String> parseSpawnId(String footerText) {
        Matcher matcher = SPAWN_ID_PATTERN.matcher(footerText);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public static Optional<String> parseBossKill(String chatLine) {
        Matcher leadingMatcher = BOSS_KILL_LEADING_PATTERN.matcher(chatLine);
        if (leadingMatcher.find()) {
            return Optional.of(leadingMatcher.group(1).trim());
        }

        Matcher trailingMatcher = BOSS_KILL_TRAILING_PATTERN.matcher(chatLine);
        return trailingMatcher.find() ? Optional.of(trailingMatcher.group(1).trim()) : Optional.empty();
    }

    public static boolean isHakiActivated(String chatLine) {
        return "You have activated haki.".equalsIgnoreCase(chatLine);
    }

    public static boolean isHakiReady(String chatLine) {
        return "You can use your haki.".equalsIgnoreCase(chatLine);
    }

    public static Optional<MoneyEvent> parseMoneyEvent(String chatLine) {
        Matcher sellMatcher = SELL_PATTERN.matcher(chatLine);
        if (sellMatcher.find()) {
            double amount = NumberParser.parse(sellMatcher.group(2), sellMatcher.group(3));
            return Optional.of(new MoneyEvent("SELL", sellMatcher.group(1).trim(), amount));
        }

        Matcher buyMatcher = BUY_PATTERN.matcher(chatLine);
        if (buyMatcher.find()) {
            double amount = NumberParser.parse(buyMatcher.group(2), buyMatcher.group(3));
            return Optional.of(new MoneyEvent("BUY", buyMatcher.group(1).trim(), amount));
        }

        return Optional.empty();
    }

    public record MoneyEvent(String type, String itemName, double amount) {
    }
}
