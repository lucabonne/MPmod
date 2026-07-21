package net.minepiece.qol.state;

import java.util.Locale;
import java.util.function.Function;
import net.minepiece.qol.util.LocalizedText;

public final class CooldownTracker {
    private static final String[] HAKI_USED_MESSAGES = {
        "You have activated haki.",
        "Has activado el haki."
    };
    private static final String[] HAKI_READY_MESSAGES = {
        "You can use your haki.",
        "Puedes usar tu haki."
    };
    private static final long HAKI_COOLDOWN_MS = 30_000L;
    private static final long HAKI_READY_VISIBLE_MS = 30_000L;

    private long hakiCooldownEndMs;
    private long hakiVisibleUntilMs;

    public void onChatMessage(String normalizedChat) {
        long now = System.currentTimeMillis();
        if (isHakiUsedMessage(normalizedChat)) {
            this.hakiCooldownEndMs = now + HAKI_COOLDOWN_MS;
            this.hakiVisibleUntilMs = this.hakiCooldownEndMs + HAKI_READY_VISIBLE_MS;
        } else if (isHakiReadyMessage(normalizedChat)) {
            this.hakiCooldownEndMs = now;
            this.hakiVisibleUntilMs = now + HAKI_READY_VISIBLE_MS;
        }
    }

    static boolean isHakiUsedMessage(String normalizedChat) {
        return LocalizedText.containsAny(normalizedChat, HAKI_USED_MESSAGES);
    }

    static boolean isHakiReadyMessage(String normalizedChat) {
        return LocalizedText.containsAny(normalizedChat, HAKI_READY_MESSAGES);
    }

    public String getHakiHudText() {
        return getHakiHudText(null);
    }

    public String getHakiHudText(Function<String, String> localizer) {
        long remainingMs = getHakiRemainingMillis();
        if (remainingMs > 0L) {
            return String.format(Locale.ROOT, localized(localizer, "cooldown.label", "Cooldown: %.1fs"), remainingMs / 1000.0D);
        }
        if (System.currentTimeMillis() < this.hakiVisibleUntilMs) {
            return localized(localizer, "cooldown.haki_ready", "Haki ready");
        }
        return "";
    }

    public boolean isHakiReady() {
        return getHakiRemainingMillis() <= 0L;
    }

    public long getHakiRemainingMillis() {
        return Math.max(0L, this.hakiCooldownEndMs - System.currentTimeMillis());
    }

    private static String localized(Function<String, String> localizer, String key, String fallback) {
        if (localizer == null) {
            return fallback;
        }
        String value = localizer.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }
}
