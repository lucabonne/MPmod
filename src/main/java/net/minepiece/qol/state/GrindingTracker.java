package net.minepiece.qol.state;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minepiece.qol.parse.ActionbarParser;

public final class GrindingTracker {
    private static final long IDLE_MS = 60_000L;
    private static final long REWARD_DISPLAY_MS = 2_500L;
    private final ZoneId zone;
    private final Map<String, DisplayedGain> previousGains = new HashMap<>();
    private final ArrayDeque<RateSample> samples = new ArrayDeque<>();
    private String day = "";
    private long lastDangerMs = -1;
    private long lastXpMs = -1;
    private long lastTickMs = -1;
    private long lastRateMs = -1;
    private long activeMs;
    private long windowStartMs;
    private double money;
    private double xp;
    private double pendingMoney;
    private double lastProfileXp = Double.NaN;
    private double xpRate = -1;
    private double moneyRate = -1;

    public GrindingTracker() { this(ZoneId.systemDefault()); }
    GrindingTracker(ZoneId zone) { this.zone = zone; }

    public void reset() {
        this.samples.clear();
        this.lastDangerMs = -1;
        this.lastXpMs = -1;
        this.lastTickMs = -1;
        this.lastRateMs = -1;
        this.activeMs = 0;
        this.windowStartMs = 0;
        this.money = 0;
        this.xp = 0;
        this.pendingMoney = 0;
        this.xpRate = -1;
        this.moneyRate = -1;
        this.lastProfileXp = Double.NaN;
    }

    public void onActionbar(String text, long now) {
        tick(now);
        double dangerDelta = 0;
        double moneyDelta = 0;
        for (ActionbarParser.SymbolGain gain : ActionbarParser.parseSymbolGains(text)) {
            if ((!gain.symbol().equals("军") && !gain.symbol().equals("实")) || !Double.isFinite(gain.value()) || gain.value() < 0) continue;
            DisplayedGain previous = this.previousGains.put(gain.symbol(), new DisplayedGain(gain.value(), now));
            double delta = previous == null || now - previous.time() > REWARD_DISPLAY_MS || gain.value() < previous.value()
                ? gain.value() : Math.max(0, gain.value() - previous.value());
            if (gain.symbol().equals("军")) dangerDelta += delta;
            else moneyDelta += delta;
        }
        if (dangerDelta > 0) {
            this.lastDangerMs = now;
            this.money += moneyDelta;
            this.pendingMoney += moneyDelta;
        }
    }

    public boolean hasRecentDanger(long now) {
        return this.lastDangerMs >= 0 && now >= this.lastDangerMs && now - this.lastDangerMs <= REWARD_DISPLAY_MS;
    }

    public void onXpGain(double gain, long now) {
        tick(now);
        if (!Double.isFinite(gain) || gain <= 0 || !hasRecentDanger(now)) return;
        if (!isActive(now)) {
            this.samples.clear();
            this.windowStartMs = this.activeMs;
        }
        this.lastXpMs = now;
        this.xp += gain;
        this.samples.addLast(new RateSample(this.activeMs, gain, this.pendingMoney));
        this.pendingMoney = 0;
        while (!this.samples.isEmpty() && this.samples.getFirst().activeMs() < this.activeMs - 60_000) this.samples.removeFirst();
        // Update only on rewards, at most every five seconds. Idle frames never change the displayed rates.
        if (this.lastRateMs < 0 || now - this.lastRateMs >= 5_000) {
            double elapsed = Math.max(10_000, Math.min(60_000, this.activeMs - this.windowStartMs));
            double recentXp = 0, recentMoney = 0;
            for (RateSample sample : this.samples) { recentXp += sample.xp(); recentMoney += sample.money(); }
            double targetXp = recentXp * 3_600_000 / elapsed;
            double targetMoney = recentMoney * 3_600_000 / elapsed;
            this.xpRate = this.xpRate < 0 ? targetXp : this.xpRate + 0.2 * (targetXp - this.xpRate);
            this.moneyRate = this.moneyRate < 0 ? targetMoney : this.moneyRate + 0.2 * (targetMoney - this.moneyRate);
            this.lastRateMs = now;
        }
    }

    /** Adjust already-counted combat XP without treating a delayed display update as a new kill. */
    public void correctXp(double correction, long now) {
        tick(now);
        if (!Double.isFinite(correction) || this.xp <= 0) return;
        double adjusted = Math.max(0, this.xp + correction);
        double applied = adjusted - this.xp;
        this.xp = adjusted;
        double recentXp = this.samples.stream().mapToDouble(RateSample::xp).sum();
        if (recentXp <= 0) return;
        double factor = Math.max(0, recentXp + applied) / recentXp;
        var corrected = new ArrayDeque<RateSample>();
        for (RateSample sample : this.samples) {
            corrected.addLast(new RateSample(sample.activeMs(), sample.xp() * factor, sample.money()));
        }
        this.samples.clear();
        this.samples.addAll(corrected);
        if (this.xpRate >= 0) this.xpRate += 0.2 * (this.xpRate * factor - this.xpRate);
    }

    public void setProfileBaseline(double value) { this.lastProfileXp = value; }

    public void observeProfileXp(double sessionXp, long now) {
        tick(now);
        if (Double.isFinite(sessionXp) && Double.isFinite(this.lastProfileXp)) onXpGain(sessionXp - this.lastProfileXp, now);
        this.lastProfileXp = sessionXp;
    }

    public void tick(long now) {
        String today = Instant.ofEpochMilli(now).atZone(this.zone).toLocalDate().toString();
        if (!today.equals(this.day)) {
            reset();
            this.day = today;
        }
        if (this.lastTickMs >= 0 && this.lastXpMs >= 0) {
            this.activeMs += Math.max(0, Math.min(now, this.lastXpMs + IDLE_MS) - this.lastTickMs);
        }
        this.lastTickMs = now;
    }

    public void pause(long now) {
        tick(now);
        this.lastXpMs = -1;
        this.lastDangerMs = -1;
        this.lastProfileXp = Double.NaN;
        this.pendingMoney = 0;
        this.samples.clear();
    }

    public boolean isActive(long now) { return this.lastXpMs >= 0 && now >= this.lastXpMs && now - this.lastXpMs < IDLE_MS; }
    public long getActiveMs() { return this.activeMs; }
    public double getMoney() { return this.money; }
    public double getXp() { return this.xp; }
    public double getXpRate() { return this.xpRate; }
    public double getMoneyRate() { return this.moneyRate; }

    public List<String> getHudLines(Function<String, String> tr, long now) {
        return List.of(
            tr.apply(isActive(now) ? "grinding.active" : "grinding.paused"),
            tr.apply("grinding.money") + ": " + ProfileXpTracker.number(this.money),
            tr.apply("progress.gained") + ": ~" + ProfileXpTracker.number(this.xp),
            "XP/h: " + (this.xpRate < 0 ? "—" : "~" + ProfileXpTracker.number(this.xpRate)),
            tr.apply("grinding.money_hour") + ": " + (this.moneyRate < 0 ? "—" : "~" + ProfileXpTracker.number(this.moneyRate)),
            tr.apply("grinding.time") + ": " + (this.activeMs / 60_000L) + "m " + (this.activeMs / 1_000L % 60) + "s"
        );
    }

    public SavedDay snapshot(long now) {
        tick(now);
        return new SavedDay(this.day, this.activeMs, this.money, this.xp, this.xpRate, this.moneyRate);
    }

    public void restore(SavedDay saved, long now) {
        reset();
        this.previousGains.clear();
        this.day = "";
        tick(now);
        if (saved != null && this.day.equals(saved.day())) {
            this.activeMs = Math.max(0, saved.activeMs());
            this.money = Math.max(0, saved.money());
            this.xp = Math.max(0, saved.xp());
            this.xpRate = saved.xpRate();
            this.moneyRate = saved.moneyRate();
        }
    }

    public record SavedDay(String day, long activeMs, double money, double xp, double xpRate, double moneyRate) { }
    private record DisplayedGain(double value, long time) { }
    private record RateSample(long activeMs, double xp, double money) { }
}
