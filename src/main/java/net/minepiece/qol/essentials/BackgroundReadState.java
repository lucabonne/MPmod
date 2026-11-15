package net.minepiece.qol.essentials;

/** One read at a time. An unrecognised menu is handed back to vanilla, never closed. */
public final class BackgroundReadState {
    public enum Outcome { WAIT, REPLAY, DELIVER, TIMEOUT }
    private boolean active;
    private int container = -1;
    private long started, opened, contentAt;
    private boolean valid, content;
    public void start(long now) { reset(); active = true; started = now; }
    public boolean claim(int id, long now) {
        if (!active || container >= 0 || now - started >= 3000) return false;
        container = id; opened = now; return true;
    }
    public boolean owns(int id) { return active && container >= 0 && container == id; }
    public void content(boolean recognised, long now) { valid = recognised; content = true; contentAt = now; }
    public void slot(long now) { if (content && valid) contentAt = now; }
    public Outcome tick(long now, boolean userScreenOpen) {
        if (!active) return Outcome.WAIT;
        if (userScreenOpen) return container >= 0 ? Outcome.REPLAY : Outcome.TIMEOUT;
        if (container < 0) return now - started >= 3000 ? Outcome.TIMEOUT : Outcome.WAIT;
        if (content && !valid || now - opened >= 5000 && !content) return Outcome.REPLAY;
        if (content && (now - contentAt >= 150 || now - opened >= 5000)) return Outcome.DELIVER;
        return Outcome.WAIT;
    }
    public boolean active() { return active; }
    public int container() { return container; }
    public void reset() { active = false; container = -1; started = opened = contentAt = 0; content = valid = false; }
}
