package net.minepiece.qol.state;

import java.util.Locale;

public final class CooldownTracker {
    // --- Haki cooldown ---
    private static final String HAKI_USED = "You have activated haki.";
    private static final String HAKI_READY = "You can use your haki.";
    private static final int HAKI_DEFAULT_SECONDS = 30;
    private static final int HAKI_READY_VISIBLE_SECONDS = 30;

    private long hakiCooldownEndMs = 0L;
    private long hakiVisibleUntilMs = 0L;

    public CooldownTracker() {
    }

    public void onChatMessage(String normalizedChat) {
        // --- Haki cooldown parsing ---
        long now = System.currentTimeMillis();
        if (normalizedChat.contains(HAKI_USED)) {
            this.hakiCooldownEndMs = now + (HAKI_DEFAULT_SECONDS * 1000L);
            this.hakiVisibleUntilMs = this.hakiCooldownEndMs + (HAKI_READY_VISIBLE_SECONDS * 1000L);
        } else if (normalizedChat.contains(HAKI_READY)) {
            this.hakiCooldownEndMs = now;
            this.hakiVisibleUntilMs = now + (HAKI_READY_VISIBLE_SECONDS * 1000L);
        }
    }

    public String getHakiHudText() {
        if (!shouldShowHakiHud()) {
            return "";
        }

        long remainingMs = getHakiRemainingMillis();
        if (remainingMs <= 0L) {
            return "Haki ready";
        }
        double seconds = remainingMs / 1000.0D;
        return String.format(Locale.ROOT, "Cooldown: %.1fs", seconds);
    }

    public boolean shouldShowHakiHud() {
        long now = System.currentTimeMillis();
        return getHakiRemainingMillis() > 0L || now < this.hakiVisibleUntilMs;
    }

    public boolean isHakiReady() {
        return getHakiRemainingSeconds() <= 0L;
    }

    public long getHakiRemainingMillis() {
        long remainingMs = this.hakiCooldownEndMs - System.currentTimeMillis();
        return Math.max(0L, remainingMs);
    }

    public long getHakiRemainingSeconds() {
        long remainingMs = getHakiRemainingMillis();
        if (remainingMs <= 0L) {
            return 0L;
        }
        return (remainingMs + 999L) / 1000L;
    }

}
