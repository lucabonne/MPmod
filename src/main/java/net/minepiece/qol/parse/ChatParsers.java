package net.minepiece.qol.parse;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.util.LocalizedText;
import net.minepiece.qol.util.NumberParser;

public final class ChatParsers {
    private static final Pattern SPAWN_ID_PATTERN = Pattern.compile("\\(spawn-([a-zA-Z0-9]+)\\)");
    private static final String LEADERBOARD_LABELS =
        "Leaderboard|Classement|Clasificacion|Clasificación|Tabla de clasificacion|Tabla de clasificación|Rangliste|Classifica|Tabela|Ranking|Liderlik";
    private static final Pattern BOSS_KILL_LEADING_PATTERN = Pattern.compile(
        "[-=]+\\s*(?:" + LEADERBOARD_LABELS + ")\\s+(.+?)\\s*[-=]+",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern BOSS_KILL_TRAILING_PATTERN = Pattern.compile(
        "[-=]+\\s*(.+?)\\s+(?:" + LEADERBOARD_LABELS + ")\\s*[-=]+",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY_AMOUNT_PATTERN = Pattern.compile("([0-9][0-9., \\u00a0]*)\\s*([KMB])?\\s*(?:实)?",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("\\s*\\(x\\d+\\)\\s*", Pattern.CASE_INSENSITIVE);
    private static final String[] AH_SELL_KEYWORDS = {
        "sold", "vendu", "vendido", "vendida", "verkauft", "venduto", "venduta", "sprzedano", "sprzedany", "dijual", "satildi", "satıldı"
    };
    private static final String[] AH_BUY_KEYWORDS = {
        "bought", "purchased", "achete", "acheté", "comprado", "comprada", "gekauft", "acquistato", "acquistata", "kupiono", "dibeli",
        "satin alindi", "satın alındı"
    };

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

    public static Optional<MoneyEvent> parseMoneyEvent(String chatLine) {
        if (chatLine == null || chatLine.isBlank()) {
            return Optional.empty();
        }

        boolean sold = LocalizedText.containsAny(chatLine, AH_SELL_KEYWORDS);
        boolean bought = LocalizedText.containsAny(chatLine, AH_BUY_KEYWORDS);
        if (!sold && !bought) {
            return Optional.empty();
        }

        Matcher amountMatcher = MONEY_AMOUNT_PATTERN.matcher(chatLine);
        MoneyAmount amount = null;
        while (amountMatcher.find()) {
            String number = amountMatcher.group(1);
            if (number == null || number.isBlank()) {
                continue;
            }
            try {
                amount = new MoneyAmount(amountMatcher.start(), amountMatcher.end(), NumberParser.parse(number, amountMatcher.group(2)));
            } catch (RuntimeException ignored) {
            }
        }
        if (amount == null || amount.value() <= 0.0D) {
            return Optional.empty();
        }

        String itemName = extractItemName(chatLine, amount.start(), amount.end());
        return Optional.of(new MoneyEvent(sold && !bought ? "SELL" : "BUY", itemName, amount.value()));
    }

    private static String extractItemName(String chatLine, int amountStart, int amountEnd) {
        String beforeAmount = chatLine.substring(0, Math.max(0, amountStart));
        String afterAmount = chatLine.substring(Math.min(chatLine.length(), amountEnd));
        String candidate = beforeAmount.length() >= afterAmount.length() ? beforeAmount : afterAmount;
        candidate = QUANTITY_PATTERN.matcher(candidate).replaceAll(" ");
        candidate = candidate.replaceAll("(?i)\\b(?:your|votre|tu|su|dein|deine|il tuo|la tua|seu|sua|twoj|twoja|item|objet|objeto|gegenstand|oggetto|przedmiot|barang|esya|eşya)\\b", " ");
        candidate = candidate.replaceAll("(?i)\\b(?:has been|a ete|a été|ha sido|wurde|e stato|è stato|foi|zostal|został|telah|basariyla|başarıyla)\\b", " ");
        candidate = candidate.replaceAll("(?i)\\b(?:sold|vendu|vendido|vendida|verkauft|venduto|venduta|sprzedano|sprzedany|dijual|satildi|satıldı|bought|purchased|achete|acheté|comprado|comprada|gekauft|acquistato|acquistata|kupiono|dibeli|satin alindi|satın alındı|for|pour|por|fur|für|per|za|untuk|icin|için|successfully|succes|éxito|erfolg|successo|powodzeniem)\\b", " ");
        candidate = candidate.replaceAll("[^\\p{L}\\p{N}' ._-]+", " ").trim().replaceAll("\\s+", " ");
        return candidate.isBlank() ? "Auction item" : candidate;
    }

    public record MoneyEvent(String type, String itemName, double amount) {
    }

    private record MoneyAmount(int start, int end, double value) {
    }
}
