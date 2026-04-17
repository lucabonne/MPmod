package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minepiece.qol.parse.ActionbarParser;

public final class JobsTracker {
    private static final long ACTIONBAR_VISIBLE_MS = 4_000L;
    private static final double XP_EPSILON = 0.0001D;
    public static final List<String> HUD_JOB_ORDER = List.of("fisherman", "farmer", "miner", "lumberjack");
    private static final ZoneId JOBS_ZONE = ZoneId.of("Europe/Rome");
    private static final Pattern LEVEL_UP_PATTERN = Pattern.compile(
        "you have reached level\\s+(\\d+)\\s+in the job\\s+([a-zA-Z]+)\\s*!",
        Pattern.CASE_INSENSITIVE
    );

    private final PersistentState state;
    private final Consumer<PersistentState> stateSaver;
    private final DebugLogManager debugLogManager;

    private double lastMoneyGain;
    private double lastXpGain;
    private double lastDisplayedMoneyGain = -1.0D;
    private double lastDisplayedXpGain = -1.0D;
    private long lastDisplayedGainMs;
    private long visibleUntilMs;
    private long lastPersistMs;

    public JobsTracker(PersistentState state, Consumer<PersistentState> stateSaver, DebugLogManager debugLogManager) {
        this.state = state;
        this.stateSaver = stateSaver;
        this.debugLogManager = debugLogManager;
    }

    public String initFromSpec(String rawSpec) {
        return initFromSpec(rawSpec, null);
    }

    public String initFromSpec(String rawSpec, Function<String, String> localizer) {
        String[] chunks = rawSpec.split(";");
        int loaded = 0;

        for (String chunk : chunks) {
            String trimmed = chunk.trim();
            if (trimmed.isBlank() || !trimmed.contains("=")) {
                continue;
            }

            try {
                String[] keyAndValue = trimmed.split("=", 2);
                String jobName = keyAndValue[0].trim().toLowerCase(Locale.ROOT);
                String[] attributes = keyAndValue[1].split(",");
                int level = -1;
                double needed = -1.0D;

                for (String attribute : attributes) {
                    String normalized = attribute.trim().toUpperCase(Locale.ROOT);
                    if (normalized.startsWith("LVL:")) {
                        level = Integer.parseInt(normalized.substring(4));
                    } else if (normalized.startsWith("NEEDED:")) {
                        needed = Double.parseDouble(normalized.substring(7));
                    }
                }

                if (level < 0 || needed < 10.0D || needed > 1_000_000_000.0D) {
                    continue;
                }

                PersistentState.JobInfo info = this.state.jobs.computeIfAbsent(jobName, key -> new PersistentState.JobInfo());
                info.level = level;
                info.neededXp = needed;
                info.currentXp = -1.0D;
                info.dailyXp = 0.0D;
                info.lastSeenMs = 0L;
                loaded++;
            } catch (RuntimeException ignored) {
                this.debugLogManager.logInternal("Skipped invalid /mpjobs init chunk: " + trimmed);
            }
        }

        this.state.currentJob = "";
        this.stateSaver.accept(this.state);
        if (loaded == 0) {
            return localized(localizer, "cmd.jobs.init_none_loaded", "No jobs loaded.");
        }
        return String.format(
            Locale.ROOT,
            localized(localizer, "cmd.jobs.init_loaded", "Loaded %d job baselines."),
            loaded
        );
    }

    public void reset() {
        for (PersistentState.JobInfo info : this.state.jobs.values()) {
            info.currentXp = -1.0D;
            info.dailyXp = 0.0D;
            info.lastSeenMs = 0L;
        }
        this.state.currentJob = "";
        this.stateSaver.accept(this.state);
    }

    public void forget() {
        this.state.jobs.clear();
        this.state.currentJob = "";
        this.stateSaver.accept(this.state);
    }

    public String resetDailyXp() {
        return resetDailyXp((Function<String, String>) null);
    }

    public String resetDailyXp(Function<String, String> localizer) {
        rolloverDailyIfNeeded();
        this.state.jobsDailyMoney = 0.0D;
        for (PersistentState.JobInfo info : this.state.jobs.values()) {
            info.dailyXp = 0.0D;
        }
        this.stateSaver.accept(this.state);
        return localized(localizer, "cmd.jobs.reset_daily_all", "Reset daily money and daily XP for all jobs.");
    }

    public String resetDailyXp(String rawJobName) {
        return resetDailyXp(rawJobName, null);
    }

    public String resetDailyXp(String rawJobName, Function<String, String> localizer) {
        if (rawJobName == null) {
            return localized(localizer, "cmd.jobs.unknown_job", "Unknown job. Use fisherman, farmer, miner, or lumberjack.");
        }

        rolloverDailyIfNeeded();
        String jobName = rawJobName.trim().toLowerCase(Locale.ROOT);
        if (!HUD_JOB_ORDER.contains(jobName)) {
            return localized(localizer, "cmd.jobs.unknown_job", "Unknown job. Use fisherman, farmer, miner, or lumberjack.");
        }

        PersistentState.JobInfo info = this.state.jobs.computeIfAbsent(jobName, key -> new PersistentState.JobInfo());
        info.dailyXp = 0.0D;
        this.stateSaver.accept(this.state);
        return String.format(
            Locale.ROOT,
            localized(localizer, "cmd.jobs.reset_daily_job", "Reset daily XP for %s."),
            displayJobName(jobName, localizer)
        );
    }

    public PersistentState.JobInfo getJob(String rawJobName) {
        if (rawJobName == null) {
            return null;
        }
        return this.state.jobs.get(rawJobName.trim().toLowerCase(Locale.ROOT));
    }

    public void setJobValues(String rawJobName, int level, double currentXp, double neededXp) {
        if (rawJobName == null) {
            return;
        }
        String jobName = rawJobName.trim().toLowerCase(Locale.ROOT);
        if (!HUD_JOB_ORDER.contains(jobName)) {
            return;
        }
        PersistentState.JobInfo info = this.state.jobs.computeIfAbsent(jobName, key -> new PersistentState.JobInfo());
        info.level = Math.max(0, level);
        info.currentXp = Math.max(0.0D, currentXp);
        info.neededXp = Math.max(0.0D, neededXp);
        info.lastSeenMs = System.currentTimeMillis();
        this.stateSaver.accept(this.state);
    }

    public String setJobBaseline(String rawJobName, int level, double neededXp) {
        return setJobBaseline(rawJobName, level, neededXp, null);
    }

    public String setJobBaseline(String rawJobName, int level, double neededXp, Function<String, String> localizer) {
        if (rawJobName == null) {
            return localized(localizer, "cmd.jobs.unknown_job", "Unknown job. Use fisherman, farmer, miner, or lumberjack.");
        }

        String jobName = rawJobName.trim().toLowerCase(Locale.ROOT);
        if (!HUD_JOB_ORDER.contains(jobName)) {
            return localized(localizer, "cmd.jobs.unknown_job", "Unknown job. Use fisherman, farmer, miner, or lumberjack.");
        }
        if (level < 0 || neededXp < 10.0D || neededXp > 1_000_000_000.0D) {
            return localized(
                localizer,
                "cmd.jobs.invalid_values",
                "Invalid values. Level must be 0+ and needed XP must be between 10 and 1000000000."
            );
        }

        PersistentState.JobInfo info = this.state.jobs.computeIfAbsent(jobName, key -> new PersistentState.JobInfo());
        info.level = level;
        info.neededXp = neededXp;
        info.currentXp = -1.0D;
        info.dailyXp = 0.0D;
        info.lastSeenMs = 0L;
        if (jobName.equals(this.state.currentJob)) {
            this.state.currentJob = "";
        }
        this.stateSaver.accept(this.state);
        return String.format(
            Locale.ROOT,
            localized(localizer, "cmd.jobs.set_baseline", "Set %s baseline to LVL %d %s XP."),
            displayJobName(jobName, localizer),
            level,
            formatXpValue(neededXp)
        );
    }

    public void captureActionbar(String actionbar) {
        captureVisibleText(actionbar);
    }

    public void captureChatMessage(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        Matcher matcher = LEVEL_UP_PATTERN.matcher(text);
        if (!matcher.find()) {
            return;
        }

        String jobName = matcher.group(2).trim().toLowerCase(Locale.ROOT);
        if (!HUD_JOB_ORDER.contains(jobName)) {
            return;
        }

        int level;
        try {
            level = Integer.parseInt(matcher.group(1));
        } catch (RuntimeException ignored) {
            return;
        }

        PersistentState.JobInfo info = this.state.jobs.computeIfAbsent(jobName, key -> new PersistentState.JobInfo());
        info.level = level;
        info.lastSeenMs = System.currentTimeMillis();
        this.state.currentJob = jobName;
        this.stateSaver.accept(this.state);
        this.debugLogManager.logInternal("Job level set from chat for " + jobName + " -> " + level);
    }

    public void captureVisibleText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String namedJob = findMentionedJob(text);
        if (!namedJob.isBlank() && !namedJob.equals(this.state.currentJob)) {
            this.state.currentJob = namedJob;
        }

        ActionbarParser.parseJobActionbar(text).ifPresent(snapshot -> {
            rolloverDailyIfNeeded();
            long now = System.currentTimeMillis();
            double moneyDelta = computeDisplayedGainDelta(snapshot.moneyGain(), this.lastDisplayedMoneyGain, now);
            double xpDelta = computeDisplayedGainDelta(snapshot.xpGain(), this.lastDisplayedXpGain, now);

            this.lastMoneyGain = snapshot.moneyGain();
            this.lastXpGain = snapshot.xpGain();
            this.lastDisplayedMoneyGain = snapshot.moneyGain();
            this.lastDisplayedXpGain = snapshot.xpGain();
            this.lastDisplayedGainMs = now;
            this.visibleUntilMs = System.currentTimeMillis() + ACTIONBAR_VISIBLE_MS;
            this.state.jobsDailyMoney += moneyDelta;

            String matchedJob = findJobForNeeded(snapshot.neededXp());
            if (!matchedJob.isBlank()) {
                this.state.currentJob = matchedJob;
            }

            if (this.state.currentJob.isBlank()) {
                return;
            }

            PersistentState.JobInfo info = this.state.jobs.get(this.state.currentJob);
            if (info == null) {
                return;
            }

            boolean resetDetected = info.currentXp >= 0.0D && snapshot.currentXp() + XP_EPSILON < info.currentXp;
            boolean neededChanged = info.neededXp > 0.0D && !sameXp(info.neededXp, snapshot.neededXp());
            boolean saneNeeded = snapshot.neededXp() >= 10.0D && snapshot.neededXp() <= 1_000_000_000.0D;

            if (saneNeeded && info.neededXp <= 0.0D) {
                info.neededXp = snapshot.neededXp();
            }

            info.currentXp = snapshot.currentXp();
            info.dailyXp += xpDelta;
            info.lastSeenMs = now;
            if (saneNeeded && (info.neededXp == 0.0D || neededChanged)) {
                info.neededXp = snapshot.neededXp();
            }

            if (neededChanged || resetDetected || now - this.lastPersistMs >= 2_000L) {
                this.lastPersistMs = now;
                this.stateSaver.accept(this.state);
            }
        });
    }

    public String summary() {
        return summary(null);
    }

    public String summary(Function<String, String> localizer) {
        if (this.state.jobs.isEmpty()) {
            return localized(localizer, "cmd.jobs.no_jobs_initialized", "No jobs initialized.");
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, PersistentState.JobInfo> entry : this.state.jobs.entrySet()) {
            PersistentState.JobInfo info = entry.getValue();
            parts.add(displayJobName(entry.getKey(), localizer) + " LVL " + info.level + " " + formatXpProgress(info));
        }
        return String.join(" | ", parts);
    }

    public String debugSummary() {
        return debugSummary(null);
    }

    public String debugSummary(Function<String, String> localizer) {
        String currentJob = this.state.currentJob.isBlank()
            ? localized(localizer, "cmd.common.none_value", "<none>")
            : displayJobName(this.state.currentJob, localizer);
        return String.format(
            Locale.ROOT,
            localized(localizer, "cmd.jobs.debug_summary", "currentJob=%s, jobs=%d, lastMoney=%.2f, lastXp=%.2f"),
            currentJob,
            this.state.jobs.size(),
            this.lastMoneyGain,
            this.lastXpGain
        );
    }

    public List<String> getOverviewHudLines() {
        return getOverviewHudLines(null);
    }

    public List<String> getOverviewHudLines(Function<String, String> localizer) {
        rolloverDailyIfNeeded();
        List<String> lines = new ArrayList<>(HUD_JOB_ORDER.size());
        for (String jobName : HUD_JOB_ORDER) {
            PersistentState.JobInfo info = this.state.jobs.get(jobName);
            lines.add(formatOverviewLine(jobName, info, localizer));
            lines.add(localized(localizer, "jobs.today_xp", "Today XP:") + " " + formatDailyValue(info == null ? 0.0D : info.dailyXp));
        }
        lines.add(localized(localizer, "jobs.today_money", "Today money:") + " " + formatDailyValue(this.state.jobsDailyMoney));
        return lines;
    }

    public List<String> getHudLines() {
        return getHudLines(null);
    }

    public List<String> getHudLines(Function<String, String> localizer) {
        if (this.state.currentJob.isBlank() || System.currentTimeMillis() > this.visibleUntilMs) {
            return List.of();
        }

        PersistentState.JobInfo info = this.state.jobs.get(this.state.currentJob);
        if (info == null) {
            return List.of();
        }

        return List.of(
            localized(localizer, "jobs.hud_job", "Job:") + " " + displayJobName(this.state.currentJob, localizer) + " LVL " + info.level,
            localized(localizer, "jobs.hud_xp", "XP:") + " " + formatXpProgress(info),
            String.format(
                Locale.ROOT,
                localized(localizer, "jobs.hud_tick", "Tick: +%.2f money / +%.2f XP"),
                this.lastMoneyGain,
                this.lastXpGain
            )
        );
    }

    private String findJobForNeeded(double neededXp) {
        for (Map.Entry<String, PersistentState.JobInfo> entry : this.state.jobs.entrySet()) {
            if (sameXp(entry.getValue().neededXp, neededXp)) {
                return entry.getKey();
            }
        }
        return "";
    }

    private static String findMentionedJob(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String jobName : HUD_JOB_ORDER) {
            if (lower.contains(jobName)) {
                return jobName;
            }
        }
        return "";
    }

    private static String capitalize(String raw) {
        if (raw.isBlank()) {
            return raw;
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static String formatOverviewLine(String jobName, PersistentState.JobInfo info, Function<String, String> localizer) {
        String label = displayJobName(jobName, localizer);
        String level = info == null || info.level <= 0 ? "?" : Integer.toString(info.level);
        return label + " LVL " + level + " " + formatXpProgress(info);
    }

    private static String displayJobName(String jobName, Function<String, String> localizer) {
        if (jobName == null || jobName.isBlank()) {
            return "";
        }
        return switch (jobName.toLowerCase(Locale.ROOT)) {
            case "fisherman" -> localized(localizer, "jobs.name.fisherman", "Fisherman");
            case "farmer" -> localized(localizer, "jobs.name.farmer", "Farmer");
            case "miner" -> localized(localizer, "jobs.name.miner", "Miner");
            case "lumberjack" -> localized(localizer, "jobs.name.lumberjack", "Lumberjack");
            default -> capitalize(jobName);
        };
    }

    private static String localized(Function<String, String> localizer, String key, String fallback) {
        if (localizer == null) {
            return fallback;
        }
        String value = localizer.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }

    private static String formatXpProgress(PersistentState.JobInfo info) {
        if (info == null) {
            return "?/?";
        }

        String current = info.currentXp < 0.0D ? "?" : formatXpValue(info.currentXp);
        String needed = info.neededXp > 0.0D ? formatXpValue(info.neededXp) : "?";
        return current + "/" + needed;
    }

    private static boolean sameXp(double left, double right) {
        return Math.abs(left - right) < XP_EPSILON;
    }

    private static String formatXpValue(double value) {
        if (Math.abs(value - Math.rint(value)) < XP_EPSILON) {
            return Long.toString(Math.round(value));
        }
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        if (formatted.indexOf('.') >= 0) {
            while (formatted.endsWith("0")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
            if (formatted.endsWith(".")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
        }
        return formatted;
    }

    private void rolloverDailyIfNeeded() {
        String todayId = LocalDate.now(JOBS_ZONE).toString();
        if (todayId.equals(this.state.jobsDayId)) {
            return;
        }

        this.state.jobsDayId = todayId;
        this.state.jobsDailyMoney = 0.0D;
        for (PersistentState.JobInfo info : this.state.jobs.values()) {
            info.dailyXp = 0.0D;
        }
        this.stateSaver.accept(this.state);
    }

    private static String formatDailyValue(double value) {
        return formatXpValue(value);
    }

    private double computeDisplayedGainDelta(double currentDisplay, double previousDisplay, long now) {
        if (currentDisplay <= 0.0D) {
            return 0.0D;
        }

        boolean sameBurst = this.lastDisplayedGainMs > 0L && now - this.lastDisplayedGainMs <= ACTIONBAR_VISIBLE_MS;
        if (!sameBurst || previousDisplay < 0.0D) {
            return currentDisplay;
        }
        if (currentDisplay + XP_EPSILON < previousDisplay) {
            return currentDisplay;
        }
        if (currentDisplay > previousDisplay + XP_EPSILON) {
            return currentDisplay - previousDisplay;
        }
        return 0.0D;
    }
}
