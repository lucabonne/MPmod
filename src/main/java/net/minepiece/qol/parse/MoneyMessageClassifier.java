package net.minepiece.qol.parse;

public final class MoneyMessageClassifier {
    private static final char[] MESSAGE_DIVIDERS = {'›', '»', '>'};

    private MoneyMessageClassifier() {
    }

    /** Server lines have no "sender › message" divider between two parts; player chat does. */
    public static boolean isEligibleServerMessage(String message, boolean overlay) {
        if (overlay || message == null || message.isBlank()) return false;
        for (char divider : MESSAGE_DIVIDERS) {
            int index = message.indexOf(divider);
            if (index >= 0) return index == 0 || index == message.length() - 1;
        }
        return true;
    }
}
