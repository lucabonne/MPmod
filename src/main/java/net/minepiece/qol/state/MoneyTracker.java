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
import java.util.function.Function;
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
    private static final Pattern ISLAND_BANK_WITHDRAW_PATTERN = Pattern.compile(
        "you have withdrawn\\s+([0-9][0-9,]*(?:\\.[0-9]+)?)([KMB])?\\s*实\\s+from your island bank",
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
    private long pendingNonAhDeltaBeforeInit;

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
        return commandSummaryLines(null);
    }

    public List<String> commandSummaryLines(Function<String, String> localizer) {
        ensureDailyRollover();
        String total = this.balanceInitializedThisSession ? formatUnsigned(this.state.money.currentBalance) : "?";
        String madeToday = this.balanceInitializedThisSession ? formatSigned(totalMadeTodayDelta()) : "?";
        return List.of(
            localized(localizer, "money.total", "Total:") + " " + total,
            localized(localizer, "money.made_today", "Made today:") + " " + madeToday,
            localized(localizer, "money.ah_sold", "AH sold:") + " " + formatUnsigned(this.state.money.ahMadeToday),
            localized(localizer, "money.ah_bought", "AH bought:") + " " + formatUnsigned(this.state.money.ahSpentToday)
        );
    }

    public String getBalanceInitializationHint() {
        return getBalanceInitializationHint(null);
    }

    public String getBalanceInitializationHint(Function<String, String> localizer) {
        return this.balanceInitializedThisSession ? "" : localized(
            localizer,
            "cmd.money.balance_hint",
            "Run /balance to initialize total."
        );
    }

    public List<String> recentLogLines(int limit) {
        return recentLogLines(limit, null);
    }

    public List<String> recentLogLines(int limit, Function<String, String> localizer) {
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
            lines.add(String.format(
                Locale.ROOT,
                localized(localizer, "cmd.money.log_entry", "[%s] %s%s 实 - %s"),
                tx.type,
                sign,
                amount,
                tx.itemName
            ));
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

    public List<PersistentState.Transaction> getHistory() {
        return this.state.money.history;
    }

    public long getCurrentBalance() {
        return this.state.money.currentBalance;
    }

    public long getMadeToday() {
        return totalMadeTodayDelta();
    }

    public long getAhMadeToday() {
        return this.state.money.ahMadeToday;
    }

    public long getAhSpentToday() {
        return this.state.money.ahSpentToday;
    }

    public long getNonAhToday() {
        return this.state.money.nonAhToday;
    }

    public boolean isBalanceInitialized() {
        return this.balanceInitializedThisSession;
    }

    public void setCurrentBalance(long value) {
        this.state.money.currentBalance = Math.max(0L, value);
        this.balanceInitializedThisSession = true;
        this.stateSaver.accept(this.state);
    }

    public void setMadeToday(long value) {
        long ahDelta = this.state.money.ahMadeToday - this.state.money.ahSpentToday;
        this.state.money.nonAhToday = value - ahDelta;
        this.stateSaver.accept(this.state);
    }

    public void setAhMadeToday(long value) {
        this.state.money.ahMadeToday = Math.max(0L, value);
        this.stateSaver.accept(this.state);
    }

    public void setAhSpentToday(long value) {
        this.state.money.ahSpentToday = Math.max(0L, value);
        this.stateSaver.accept(this.state);
    }

    public boolean removeTransaction(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        boolean removed = this.state.money.history.removeIf(tx -> id.equals(tx.id));
        if (removed) {
            this.state.money.transactions = this.state.money.history.size();
            recomputeTodayFromHistory();
            this.stateSaver.accept(this.state);
        }
        return removed;
    }

    public boolean editTransaction(String id, String type, String itemName, double amount) {
        if (id == null || id.isBlank()) {
            return false;
        }
        for (PersistentState.Transaction tx : this.state.money.history) {
            if (id.equals(tx.id)) {
                tx.type = type == null ? tx.type : type.toUpperCase(Locale.ROOT);
                tx.itemName = itemName == null ? tx.itemName : itemName;
                tx.amount = amount;
                recomputeTodayFromHistory();
                this.stateSaver.accept(this.state);
                return true;
            }
        }
        return false;
    }

    public void recomputeTodayFromHistory() {
        String todayId = LocalDate.now(MONEY_ZONE).toString();
        long ahMade = 0L;
        long ahSpent = 0L;
        for (PersistentState.Transaction tx : this.state.money.history) {
            String txDay = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(tx.epochMs), MONEY_ZONE).toString();
            if (!todayId.equals(txDay)) {
                continue;
            }
            long rounded = Math.max(0L, Math.round(tx.amount));
            if ("SELL".equalsIgnoreCase(tx.type)) {
                ahMade += rounded;
            } else if ("BUY".equalsIgnoreCase(tx.type)) {
                ahSpent += rounded;
            }
        }
        this.state.money.ahMadeToday = ahMade;
        this.state.money.ahSpentToday = ahSpent;
    }

    public void resetAll() {
        this.awaitingBalanceReplyUntilMs = 0L;
        this.balanceInitializedThisSession = false;
        this.lastActionbarMoneyDisplay = -1L;
        this.lastActionbarDisplayMs = 0L;
        this.lastNonAhAppliedMs = 0L;
        this.lastNonAhAppliedDelta = 0L;
        this.lastNonAhAppliedSource = "";
        this.pendingNonAhDeltaBeforeInit = 0L;

        this.state.money.currentBalance = 0L;
        this.state.money.lastLoggedBalance = 0L;
        this.state.money.dayStartBalance = 0L;
        this.state.money.dayId = "";
        this.state.money.nonAhToday = 0L;
        this.state.money.ahSpentToday = 0L;
        this.state.money.ahMadeToday = 0L;
        this.state.money.transactions = 0;
        this.state.money.history.clear();
        this.stateSaver.accept(this.state);
    }

    public List<String> getHudLines() {
        return getHudLines(null);
    }

    public List<String> getHudLines(Function<String, String> localizer) {
        ensureDailyRollover();
        List<String> lines = new ArrayList<>(4);
        lines.add(localized(localizer, "money.total", "Total:") + " " + (this.balanceInitializedThisSession
            ? formatCompact(this.state.money.currentBalance)
            : "?"));
        lines.add(localized(localizer, "money.made_today", "Made today:") + " " + (this.balanceInitializedThisSession
            ? formatSignedCompact(totalMadeTodayDelta())
            : "?"));
        lines.add(localized(localizer, "money.ah_sold", "AH sold:") + " " + formatCompact(Math.max(0L, this.state.money.ahMadeToday)));
        lines.add(localized(localizer, "money.ah_bought", "AH bought:") + " " + formatCompact(Math.max(0L, this.state.money.ahSpentToday)));
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
        if (this.pendingNonAhDeltaBeforeInit != 0L) {
            this.state.money.currentBalance = Math.max(0L, this.state.money.currentBalance + this.pendingNonAhDeltaBeforeInit);
            this.state.money.nonAhToday += this.pendingNonAhDeltaBeforeInit;
            this.pendingNonAhDeltaBeforeInit = 0L;
        }
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
        if (signedDelta == 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!this.lastNonAhAppliedSource.equals(source)
            && now - this.lastNonAhAppliedMs <= CROSS_SOURCE_DEDUPE_MS
            && this.lastNonAhAppliedDelta == signedDelta) {
            return;
        }

        this.lastNonAhAppliedMs = now;
        this.lastNonAhAppliedDelta = signedDelta;
        this.lastNonAhAppliedSource = source;
        if (!this.balanceInitializedThisSession) {
            this.pendingNonAhDeltaBeforeInit += signedDelta;
            return;
        }

        this.state.money.currentBalance += signedDelta;
        this.state.money.nonAhToday += signedDelta;
        this.stateSaver.accept(this.state);
    }

    private Optional<Long> parseChatMoneyDelta(String line) {
        Optional<Long> positive = firstMatchAmount(line,
            ISLAND_BANK_WITHDRAW_PATTERN, INVENTORY_SOLD_PATTERN,
            PET_BROUGHT_YOU_PATTERN, POSITIVE_CHAT_PATTERN);
        if (positive.isPresent()) {
            return positive;
        }
        return firstMatchAmount(line, NEGATIVE_CHAT_PATTERN).map(amount -> -amount);
    }

    private static Optional<Long> firstMatchAmount(String line, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                long amount = Math.round(NumberParser.parse(matcher.group(1), matcher.group(2)));
                return amount > 0L ? Optional.of(amount) : Optional.empty();
            }
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

    private static String formatCompact(long value) {
        String[] units = {"", "k", "M", "B", "T"};
        long abs = Math.abs(value);
        if (abs < 1_000L) {
            return Long.toString(value);
        }

        double scaled = abs;
        int unitIndex = 0;
        while (scaled >= 1_000.0D && unitIndex < units.length - 1) {
            scaled /= 1_000.0D;
            unitIndex++;
        }

        // Avoid 1000.00k / 1000.00M after rounding by promoting to next unit.
        double rounded = Math.round(scaled * 100.0D) / 100.0D;
        if (rounded >= 1_000.0D && unitIndex < units.length - 1) {
            rounded /= 1_000.0D;
            unitIndex++;
        }

        String sign = value < 0L ? "-" : "";
        return String.format(Locale.ROOT, "%s%.2f%s", sign, rounded, units[unitIndex]);
    }

    private static String formatSignedCompact(long value) {
        if (value == 0L) {
            return "0";
        }
        return (value > 0L ? "+" : "-") + formatCompact(Math.abs(value));
    }

    private static String csv(String raw) {
        String safe = raw == null ? "" : raw;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static String localized(Function<String, String> localizer, String key, String fallback) {
        if (localizer == null) {
            return fallback;
        }
        String value = localizer.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }
}
