package net.minepiece.qol.telemetry;

import java.util.Locale;
import java.util.Set;

public final class TelemetryRedactor {
    private static final Set<String> SECRET_COMMANDS = Set.of(
        "login", "l", "register", "reg", "password", "changepassword", "auth", "authenticate", "verify", "verification", "2fa", "otp"
    );

    private TelemetryRedactor() {
    }

    public static String redactCommand(String command) {
        if (command == null) {
            return "";
        }
        String trimmed = command.trim();
        boolean slash = trimmed.startsWith("/");
        String body = slash ? trimmed.substring(1) : trimmed;
        int separator = body.indexOf(' ');
        String root = (separator < 0 ? body : body.substring(0, separator)).toLowerCase(Locale.ROOT);
        if (!SECRET_COMMANDS.contains(root)) {
            return trimmed;
        }
        return (slash ? "/" : "") + root + (separator < 0 ? "" : " <redacted>");
    }

    public static String excelSafe(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' ? "'" + value : value;
    }
}
