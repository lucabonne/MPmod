package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minepiece.qol.parse.ProfileXpParser;

public final class ProfileXpTracker {
    private ProfileXpParser.Snapshot anchor;
    private double gainedAtAnchor;
    private double currentXp;
    private double percent;
    private Integer displayedLevel;
    private boolean needsSync;
    private boolean estimated;
    private boolean incomplete;
    private boolean cached;

    public void reset() {
        this.anchor = null;
        this.gainedAtAnchor = 0;
        this.currentXp = 0;
        this.percent = 0;
        this.displayedLevel = null;
        this.needsSync = false;
        this.estimated = false;
        this.incomplete = false;
        this.cached = false;
    }

    public void sync(ProfileXpParser.Snapshot snapshot) {
        if (this.anchor != null) {
            if (snapshot.ascension() != this.anchor.ascension()) {
                reset();
            } else if (snapshot.level() == this.anchor.level()) {
                this.gainedAtAnchor += snapshot.currentXp() - this.anchor.currentXp();
            } else if (snapshot.level() == this.anchor.level() + 1) {
                this.gainedAtAnchor += this.anchor.requiredXp() - this.anchor.currentXp() + snapshot.currentXp();
            } else {
                // XP requirements for skipped levels are unknown; never invent earned XP.
                this.incomplete = true;
            }
        }
        this.gainedAtAnchor = Math.max(0, this.gainedAtAnchor);
        this.anchor = snapshot;
        this.currentXp = snapshot.currentXp();
        this.percent = snapshot.percent();
        this.displayedLevel = snapshot.level();
        this.needsSync = false;
        this.estimated = false;
        this.cached = false;
    }

    public void addSharedXp(double gain) {
        if (needsSync() || !Double.isFinite(gain) || gain <= 0) return;
        this.currentXp += gain;
        this.estimated = true;
        if (this.currentXp >= this.anchor.requiredXp()) {
            this.needsSync = true;
            this.displayedLevel = this.anchor.level() + 1;
            return;
        }
        this.percent = this.currentXp / this.anchor.requiredXp() * 100;
    }

    public boolean isCached() { return this.cached; }

    public record SavedProfile(ProfileXpParser.Snapshot snapshot, boolean needsSync, Integer displayedLevel) { }

    public SavedProfile snapshot() {
        if (this.anchor == null) return null;
        var saved = needsSync() ? this.anchor : new ProfileXpParser.Snapshot(this.anchor.ascension(),
            this.anchor.level(), this.currentXp, this.anchor.requiredXp());
        return new SavedProfile(saved, this.needsSync, this.displayedLevel);
    }

    public void restore(SavedProfile saved) {
        reset();
        if (saved == null || saved.snapshot() == null || saved.snapshot().requiredXp() <= 0) return;
        sync(saved.snapshot());
        this.needsSync = saved.needsSync();
        this.displayedLevel = saved.displayedLevel();
        this.cached = true;
        this.estimated = true;
    }

    public void updatePercent(ProfileXpParser.BarProgress progress) { updatePercent(progress, true); }

    public void updatePercent(ProfileXpParser.BarProgress progress, boolean applyXp) {
        if (this.anchor == null) {
            return;
        }
        Integer level = progress.level();
        if (level != null) this.cached = false;
        if (!applyXp && !this.needsSync) {
            if (level != null && level != this.anchor.level()) {
                this.needsSync = true;
                this.displayedLevel = level;
            }
            return;
        }
        double displayedPercent = Math.round(this.percent * 100.0D) / 100.0D;
        if (progress.percent() == displayedPercent && (level == null || level.equals(this.displayedLevel))) {
            return;
        }
        if ((level != null && level != this.anchor.level()) || progress.percent() + 0.011D < displayedPercent) {
            this.needsSync = true;
            this.displayedLevel = level;
        }
        this.percent = progress.percent();
        if (this.needsSync) {
            return;
        }
        double anchorPercent = Math.round(this.anchor.percent() * 100.0D) / 100.0D;
        this.currentXp = Math.max(0, Math.min(this.anchor.requiredXp(),
            this.anchor.currentXp() + (this.percent - anchorPercent) / 100.0D * this.anchor.requiredXp()));
        this.estimated = true;
    }

    /** Reconcile a fresh server reading; a lower percentage at the same level is a correction, not a level-up. */
    public void reconcilePercent(ProfileXpParser.BarProgress progress) {
        if (this.anchor == null) return;
        if (progress.level() == null) {
            updatePercent(progress);
            return;
        }
        this.cached = false;
        this.displayedLevel = progress.level();
        if (progress.level() != this.anchor.level()) {
            this.needsSync = true;
            return;
        }
        this.needsSync = false;
        if (Math.round(this.percent * 100.0D) / 100.0D != progress.percent()) {
            this.currentXp = progress.percent() / 100.0D * this.anchor.requiredXp();
            this.percent = progress.percent();
            this.estimated = true;
        }
    }

    public ProfileXpParser.Snapshot getAnchor() {
        return this.anchor;
    }

    public boolean needsSync() {
        return this.anchor == null || this.needsSync;
    }

    public double getGainedXp() {
        return needsSync() || this.incomplete ? Double.NaN
            : Math.max(0, this.gainedAtAnchor + this.currentXp - this.anchor.currentXp());
    }

    public double getCurrentXp() {
        return needsSync() ? Double.NaN : this.currentXp;
    }

    public List<String> getHudLines(Function<String, String> tr) {
        if (this.anchor == null) {
            return List.of(tr.apply("progress.sync_hint"));
        }
        List<String> lines = new ArrayList<>();
        lines.add(tr.apply("progress.level") + ": " + (this.displayedLevel == null ? "?" : this.displayedLevel)
            + (needsSync() ? "" : String.format(Locale.ROOT, " (%.2f%%)", this.percent)));
        if (needsSync()) {
            lines.add(tr.apply("progress.level_sync_hint"));
        } else {
            lines.add("XP: " + (this.estimated ? "~" : "") + number(this.currentXp) + " / " + number(this.anchor.requiredXp()));
        }
        if (this.cached) lines.add(tr.apply("progress.cached_hint"));
        return lines;
    }

    static String number(double value) {
        return String.format(Locale.ROOT, "%,.0f", value);
    }
}
