package net.minepiece.qol.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ClockHud {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private ClockHud() { }

    public static String format(Instant instant, ZoneId zone) {
        return FORMAT.format(instant.atZone(zone));
    }

    public static List<String> lines() {
        return List.of(format(Instant.now(), ZoneId.systemDefault()));
    }
}
