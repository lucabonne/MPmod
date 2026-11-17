package net.minepiece.qol.parse;

import net.minepiece.qol.state.ChatTranslationManager;

public final class MoneyMessageClassifier {
    private MoneyMessageClassifier() {
    }

    public static boolean isEligibleServerMessage(String message, boolean overlay) {
        return !overlay && ChatTranslationManager.classifyLine(message).lineType()
            == ChatTranslationManager.ChatLineType.SYSTEM;
    }
}
