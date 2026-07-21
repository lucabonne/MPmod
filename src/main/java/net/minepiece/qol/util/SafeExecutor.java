package net.minepiece.qol.util;

import net.minepiece.qol.state.DebugLogManager;

public final class SafeExecutor {
    private SafeExecutor() {
    }

    public static void run(DebugLogManager debugLogManager, String scope, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            if (debugLogManager != null) {
                debugLogManager.logInternal("Parser failure in " + scope + ": " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
            }
        }
    }
}
