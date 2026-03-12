package net.minepiece.qol.state;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.parse.ChatParsers;
import net.minepiece.qol.util.NumberParser;

public final class MoneyTracker {
    private static final long BALANCE_REPLY_WINDOW_MS = 5_000L;
    private static final long ACTIONBAR_VISIBLE_MS = 4_000L;
    private static final long CROSS_SOURCE_DEDUPE_MS = 2_000L;
    private static final int MAX_HISTORY = 2_000;
    private static final ZoneId MONEY_ZONE = ZoneId.of("Europe/Rome");
    private static final Pattern MONEY_TOKEN_PATTERN = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)([KMB])?\\s*实", Pattern.CASE_INSENSITIVE);
    private static final Pattern POSITIVE_CHAT_PATTERN = Pattern.compile(
        "(?:increased by|received|gained|earned|won)\\s+([0-9][0-9,]*(?:\\.[0-9]+)?)([KMB])?\\s*实",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PET_BROUGHT_YOU_PATTERN = Pattern.compile(
        "your pet\\s+.+?\\s+brought you\\s+([0-9][0-9,]*(?:\\.[0-9]+)?)([KMB])?\\s*实",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INVENTORY_SOLD_PATTERN = Pattern.compile(
        "you sold the contents of your inventory for\\s+([0-9][0-9,]*(?:\\.[0-9]+)?)([KMB])?\\s*实",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NEGATIVE_CHAT_PATTERN = Pattern.compile(
        "(?:lost|withdrawn|withdrew|paid|sent|decreased by)\\s+([0-9][0-9,]*(?:\\.[0-9]+)?)([KMB])?\\s*实",
        Pattern.CASE_INSENSITIVE
    );

    private final PersistentState state;
    private final Consumer<PersistentState> stateSaver;
    private final Path baseDir;

    private long awaitingBalanceReplyUntilMs;
    private boolean balanceInitializedThisSession;
    private long lastActionbarMoneyDisplay = -1L;
    private long lastActionbarDisplayMs;
    private long lastNonAhAppliedMs;
    private long lastNonAhAppliedDelta;
    private String lastNonAhAppliedSource = "";

    public MoneyTracker(PersistentState state, Consumer<PersistentState> stateSaver, Path baseDir) {
        this.state = state;
        this.stateSaver = stateSaver;
        this.baseDir = baseDir;
    }

    public void onCommandSent(String command) {
        String root = commandRoot(command);
        if ("balance".equalsIgnoreCase(root)) {
            this.awaitingBalanceReplyUntilMs = System.currentTimeMillis() + BALANCE_REPLY_WINDOW_MS;
        }
    }

    public void onChatMessage(String chatLine) {
        if (chatLine == null || chatLine.isBlank()) {
            return;
        }

        ensureDailyRollover();

        if (handlePendingBalanceReply(chatLine)) {
            return;
        }

        Optional<ChatParsers.MoneyEvent> maybeMoneyEvent = ChatParsers.parseMoneyEvent(chatLine);
        if (maybeMoneyEvent.isPresent()) {
            handleAhTransaction(maybeMoneyEvent.get());
            return;
        }

        parseChatMoneyDelta(chatLine).ifPresent(delta -> applyNonAhDelta(delta, "chat"));
    }

    public void onActionbarMessage(String actionbarLine) {
        if (actionbarLine == null || actionbarLine.isBlank()) {
            return;
        }

        ensureDailyRollover();
        long now = System.currentTimeMillis();
        long displayValue = parseLargestMoneyToken(actionbarLine);
        if (displayValue <= 0L) {
            return;
        }

        long delta = computeDisplayedGainDelta(displayValue, now);
        this.lastActionbarMoneyDisplay = displayValue;
        this.lastActionbarDisplayMs = now;
        if (delta > 0L) {
            applyNonAhDelta(delta, "actionbar");
        }
    }

    public List<String> commandSummaryLines() {
        ensureDailyRollover();
        List<String> lines = new ArrayList<>(4);
        lines.add("Total: " + formatTotalValue());
        lines.add("Made today: " + formatMadeTodayValue());
        lines.add("AH sold: " + formatUnsigned(this.state.money.ahMadeToday));
        lines.add("AH bought: " + formatUnsigned(this.state.money.ahSpentToday));
        return lines;
    }

    public String getBalanceInitializationHint() {
        return this.balanceInitializedThisSession ? "" : "Run /balance to initialize total.";
    }

    public List<String> recentLogLines(int limit) {
        if (limit <= 0 || this.state.money.history.isEmpty()) {
            return List.of();
        }

        int size = this.state.money.history.size();
        int startIndex = Math.max(0, size - limit);
        List<String> lines = new ArrayList<>(size - startIndex);
        for (int i = size - 1; i >= startIndex; i--) {
            PersistentState.Transaction tx = this.state.money.history.get(i);
            String sign = "SELL".equalsIgnoreCase(tx.type) ? "+" : "-";
            String amount = String.format(Locale.ROOT, "%,d", Math.round(tx.amount));
            lines.add(String.format(Locale.ROOT, "[%s] %s%s 实 - %s", tx.type, sign, amount, tx.itemName));
        }
        return lines;
    }

    public String exportCsv() {
        Path exportPath = this.baseDir.resolve("money-history.csv");
        List<String> lines = new ArrayList<>(this.state.money.history.size() + 1);
        lines.add("id,type,itemName,amount,epochMs");
        for (PersistentState.Transaction tx : this.state.money.history) {
            String row = csv(tx.id) + "," + csv(tx.type) + "," + csv(tx.itemName) + ","
                + String.format(Locale.ROOT, "%.2f", tx.amount) + "," + tx.epochMs;
            lines.add(row);
        }
        try {
            Files.createDirectories(this.baseDir);
            Files.write(exportPath, lines);
            return exportPath.toString();
        } catch (IOException ignored) {
            return null;
        }
    }

    public void sync() {
        this.stateSaver.accept(this.state);
    }

    public List<String> getHudLines() {
        ensureDailyRollover();
        List<String> lines = new ArrayList<>(4);
        lines.add("Total: " + (this.balanceInitializedThisSession
            ? String.format(Locale.ROOT, "%,d", this.state.money.currentBalance)
            : "?"));
        lines.add("Made today: " + (this.balanceInitializedThisSession
            ? formatSignedPlain(totalMadeTodayDelta())
            : "?"));
        lines.add("AH sold: " + String.format(Locale.ROOT, "%,d", Math.max(0L, this.state.money.ahMadeToday)));
        lines.add("AH bought: " + String.format(Locale.ROOT, "%,d", Math.max(0L, this.state.money.ahSpentToday)));
        return lines;
    }

    private boolean handlePendingBalanceReply(String chatLine) {
        long now = System.currentTimeMillis();
        if (this.awaitingBalanceReplyUntilMs <= 0L) {
            return false;
        }
        if (now > this.awaitingBalanceReplyUntilMs) {
            this.awaitingBalanceReplyUntilMs = 0L;
            return false;
        }

        long parsedBalance = parseLargestMoneyToken(chatLine);
        if (parsedBalance <= 0L) {
            return false;
        }

        applyBalanceUpdate(parsedBalance);
        this.awaitingBalanceReplyUntilMs = 0L;
        return true;
    }

    private void applyBalanceUpdate(long parsedBalance) {
        String todayId = LocalDate.now(MONEY_ZONE).toString();
        if (!todayId.equals(this.state.money.dayId)) {
            this.state.money.dayId = todayId;
            this.state.money.ahSpentToday = 0L;
            this.state.money.ahMadeToday = 0L;
            this.state.money.nonAhToday = 0L;
            this.state.money.dayStartBalance = parsedBalance;
        } else if (this.state.money.dayStartBalance <= 0L) {
            this.state.money.dayStartBalance = parsedBalance;
        }

        long previous = this.state.money.currentBalance;
        this.state.money.lastLoggedBalance = previous > 0L ? previous : 0L;
        this.state.money.currentBalance = parsedBalance;
        this.balanceInitializedThisSession = true;
        this.stateSaver.accept(this.state);
    }

    private void handleAhTransaction(ChatParsers.MoneyEvent event) {
        long now = System.currentTimeMillis();
        long rounded = Math.max(0L, Math.round(event.amount()));

        PersistentState.Transaction tx = new PersistentState.Transaction();
        tx.id = UUID.randomUUID().toString();
        tx.type = event.type();
        tx.itemName = event.itemName();
        tx.amount = event.amount();
        tx.epochMs = now;
        this.state.money.history.add(tx);
        if (this.state.money.history.size() > MAX_HISTORY) {
            this.state.money.history.remove(0);
        }
        this.state.money.transactions = this.state.money.history.size();

        if ("SELL".equalsIgnoreCase(event.type())) {
            this.state.money.ahMadeToday += rounded;
            if (this.balanceInitializedThisSession) {
                this.state.money.currentBalance += rounded;
            }
        } else if ("BUY".equalsIgnoreCase(event.type())) {
            this.state.money.ahSpentToday += rounded;
            if (this.balanceInitializedThisSession) {
                this.state.money.currentBalance -= rounded;
            }
        }
        this.stateSaver.accept(this.state);
    }

    private void applyNonAhDelta(long signedDelta, String source) {
        if (signedDelta == 0L || !this.balanceInitializedThisSession) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!this.lastNonAhAppliedSource.equals(source)
            && now - this.lastNonAhAppliedMs <= CROSS_SOURCE_DEDUPE_MS
            && this.lastNonAhAppliedDelta == signedDelta) {
            return;
        }

        this.state.money.currentBalance += signedDelta;
        this.state.money.nonAhToday += signedDelta;
        this.lastNonAhAppliedMs = now;
        this.lastNonAhAppliedDelta = signedDelta;
        this.lastNonAhAppliedSource = source;
        this.stateSaver.accept(this.state);
    }

    private Optional<Long> parseChatMoneyDelta(String line) {
        Matcher inventorySoldMatcher = INVENTORY_SOLD_PATTERN.matcher(line);
        if (inventorySoldMatcher.find()) {
            long amount = Math.round(NumberParser.parse(inventorySoldMatcher.group(1), inventorySoldMatcher.group(2)));
            return amount > 0L ? Optional.of(amount) : Optional.empty();
        }

        Matcher petBroughtMatcher = PET_BROUGHT_YOU_PATTERN.matcher(line);
        if (petBroughtMatcher.find()) {
            long amount = Math.round(NumberParser.parse(petBroughtMatcher.group(1), petBroughtMatcher.group(2)));
            return amount > 0L ? Optional.of(amount) : Optional.empty();
        }

        Matcher positiveMatcher = POSITIVE_CHAT_PATTERN.matcher(line);
        if (positiveMatcher.find()) {
            long amount = Math.round(NumberParser.parse(positiveMatcher.group(1), positiveMatcher.group(2)));
            return amount > 0L ? Optional.of(amount) : Optional.empty();
        }

        Matcher negativeMatcher = NEGATIVE_CHAT_PATTERN.matcher(line);
        if (negativeMatcher.find()) {
            long amount = Math.round(NumberParser.parse(negativeMatcher.group(1), negativeMatcher.group(2)));
            return amount > 0L ? Optional.of(-amount) : Optional.empty();
        }
        return Optional.empty();
    }

    private long computeDisplayedGainDelta(long currentDisplay, long now) {
        if (currentDisplay <= 0L) {
            return 0L;
        }
        boolean sameBurst = this.lastActionbarDisplayMs > 0L && now - this.lastActionbarDisplayMs <= ACTIONBAR_VISIBLE_MS;
        if (!sameBurst || this.lastActionbarMoneyDisplay < 0L) {
            return currentDisplay;
        }
        if (currentDisplay < this.lastActionbarMoneyDisplay) {
            return currentDisplay;
        }
        if (currentDisplay > this.lastActionbarMoneyDisplay) {
            return currentDisplay - this.lastActionbarMoneyDisplay;
        }
        return 0L;
    }

    private void ensureDailyRollover() {
        String todayId = LocalDate.now(MONEY_ZONE).toString();
        if (todayId.equals(this.state.money.dayId)) {
            return;
        }

        this.state.money.dayId = todayId;
        this.state.money.ahSpentToday = 0L;
        this.state.money.ahMadeToday = 0L;
        this.state.money.nonAhToday = 0L;
        this.state.money.dayStartBalance = this.balanceInitializedThisSession ? this.state.money.currentBalance : 0L;
        this.stateSaver.accept(this.state);
    }

    private static String commandRoot(String command) {
        if (command == null) {
            return "";
        }
        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int firstSpace = normalized.indexOf(' ');
        return firstSpace >= 0 ? normalized.substring(0, firstSpace) : normalized;
    }

    private static long parseLargestMoneyToken(String text) {
        Matcher matcher = MONEY_TOKEN_PATTERN.matcher(text);
        long max = 0L;
        while (matcher.find()) {
            try {
                long parsed = Math.round(NumberParser.parse(matcher.group(1), matcher.group(2)));
                if (parsed > max) {
                    max = parsed;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return max;
    }

    private String formatTotalValue() {
        return this.balanceInitializedThisSession
            ? formatUnsigned(this.state.money.currentBalance)
            : "?";
    }

    private String formatMadeTodayValue() {
        return this.balanceInitializedThisSession
            ? formatSigned(totalMadeTodayDelta())
            : "?";
    }

    private long totalMadeTodayDelta() {
        return this.state.money.nonAhToday + this.state.money.ahMadeToday - this.state.money.ahSpentToday;
    }

    private static String formatUnsigned(long value) {
        return String.format(Locale.ROOT, "%,d 实", Math.max(0L, value));
    }

    private static String formatSigned(long value) {
        if (value == 0L) {
            return "0 实";
        }
        return String.format(Locale.ROOT, "%s%,d 实", value > 0L ? "+" : "-", Math.abs(value));
    }

    private static String formatSignedPlain(long value) {
        if (value == 0L) {
            return "0";
        }
        return String.format(Locale.ROOT, "%s%,d", value > 0L ? "+" : "-", Math.abs(value));
    }

    private static String csv(String raw) {
        String safe = raw == null ? "" : raw;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
