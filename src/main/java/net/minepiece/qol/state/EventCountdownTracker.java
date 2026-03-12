package net.minepiece.qol.state;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public final class EventCountdownTracker {
    private static final ZoneId EVENT_ZONE = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private static final List<ScheduledEvent> DEFAULT_EVENTS = List.of(
        new ScheduledEvent("Buster Call", LocalTime.of(1, 0)),
        new ScheduledEvent("Auction", LocalTime.of(2, 30)),
        new ScheduledEvent("Skypea Race", LocalTime.of(3, 30)),
        new ScheduledEvent("Pumpkin Hunt", LocalTime.of(15, 0)),
        new ScheduledEvent("Pumpkin Hunt", LocalTime.of(18, 0)),
        new ScheduledEvent("Auction", LocalTime.of(18, 30)),
        new ScheduledEvent("Buster Call", LocalTime.of(19, 0)),
        new ScheduledEvent("Skypea Race", LocalTime.of(20, 30)),
        new ScheduledEvent("Pumpkin Hunt", LocalTime.of(21, 0)),
        new ScheduledEvent("Buster Call", LocalTime.of(22, 0))
    );

    private final PersistentState state;
    private final Consumer<PersistentState> stateSaver;

    private String nextName = "Unknown";
    private long remainingMillis = 0L;
    private ZonedDateTime nextAt = ZonedDateTime.now(EVENT_ZONE).plusHours(1L);

    public EventCountdownTracker(PersistentState state, Consumer<PersistentState> stateSaver) {
        this.state = state;
        this.stateSaver = stateSaver;
        ensureDefaults();
        tick();
    }

    public void tick() {
        ZonedDateTime now = ZonedDateTime.now(EVENT_ZONE);
        List<ScheduledEvent> events = getOrderedEvents();
        if (events.isEmpty()) {
            this.nextName = "No events";
            this.nextAt = now;
            this.remainingMillis = 0L;
            return;
        }
        NextEvent next = computeNext(now, events);
        this.nextName = next.name();
        this.nextAt = next.at();
        this.remainingMillis = Math.max(0L, Duration.between(now, next.at()).toMillis());
    }

    public String getDisplayLine() {
        if (this.nextAt == null || !this.nextAt.isAfter(ZonedDateTime.now(EVENT_ZONE))) {
            tick();
        }

        long totalSeconds = Math.max(0L, this.remainingMillis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "Next: %s (%d:%02d:%02d)", this.nextName, hours, minutes, seconds);
    }

    public String getNextName() {
        return this.nextName;
    }

    public long getRemainingSeconds() {
        return Math.max(0L, this.remainingMillis / 1000L);
    }

    public ZonedDateTime getNextAt() {
        return this.nextAt;
    }

    public String addEvent(String name, String time) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            return "Event name is required.";
        }
        Optional<LocalTime> parsedTime = parseTime(time);
        if (parsedTime.isEmpty()) {
            return "Invalid time. Use CET format HH:mm (e.g. 19:30).";
        }
        LocalTime localTime = parsedTime.get();
        for (PersistentState.ScheduledEvent existing : this.state.scheduledEvents) {
            if (existing == null) {
                continue;
            }
            if (cleanName.equalsIgnoreCase(safeTrim(existing.name))
                && TIME_FORMAT.format(localTime).equals(safeTrim(existing.time))) {
                return "Event already exists.";
            }
        }

        PersistentState.ScheduledEvent event = new PersistentState.ScheduledEvent();
        event.name = cleanName;
        event.time = TIME_FORMAT.format(localTime);
        this.state.scheduledEvents.add(event);
        sortStateEvents(this.state.scheduledEvents);
        this.stateSaver.accept(this.state);
        tick();
        return "Added event: " + cleanName + " at " + event.time + " CET.";
    }

    public String removeEvent(String name, String time) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            return "Event name is required.";
        }
        Optional<LocalTime> parsedTime = parseTime(time);
        if (parsedTime.isEmpty()) {
            return "Invalid time. Use CET format HH:mm (e.g. 19:30).";
        }
        String normalizedTime = TIME_FORMAT.format(parsedTime.get());
        boolean removed = this.state.scheduledEvents.removeIf(event -> event != null
            && cleanName.equalsIgnoreCase(safeTrim(event.name))
            && normalizedTime.equals(safeTrim(event.time)));
        if (!removed) {
            return "No matching event found for " + cleanName + " at " + normalizedTime + " CET.";
        }

        sortStateEvents(this.state.scheduledEvents);
        this.stateSaver.accept(this.state);
        tick();
        return "Removed event: " + cleanName + " at " + normalizedTime + " CET.";
    }

    public List<String> infoLines() {
        List<ScheduledEvent> events = getOrderedEvents();
        if (events.isEmpty()) {
            return List.of("No events configured.");
        }
        List<String> lines = new ArrayList<>();
        lines.add("Events (CET):");
        for (ScheduledEvent event : events) {
            lines.add(TIME_FORMAT.format(event.time()) + " - " + event.name());
        }
        return lines;
    }

    private void ensureDefaults() {
        if (this.state.scheduledEvents == null) {
            this.state.scheduledEvents = new ArrayList<>();
        }
        if (!this.state.scheduledEvents.isEmpty()) {
            sortStateEvents(this.state.scheduledEvents);
            return;
        }
        for (ScheduledEvent event : DEFAULT_EVENTS) {
            PersistentState.ScheduledEvent stored = new PersistentState.ScheduledEvent();
            stored.name = event.name();
            stored.time = TIME_FORMAT.format(event.time());
            this.state.scheduledEvents.add(stored);
        }
        this.stateSaver.accept(this.state);
    }

    private List<ScheduledEvent> getOrderedEvents() {
        List<ScheduledEvent> events = new ArrayList<>();
        for (PersistentState.ScheduledEvent stored : this.state.scheduledEvents) {
            if (stored == null) {
                continue;
            }
            String name = safeTrim(stored.name);
            if (name.isBlank()) {
                continue;
            }
            parseTime(stored.time).ifPresent(time -> events.add(new ScheduledEvent(name, time)));
        }
        events.sort(Comparator.comparing(ScheduledEvent::time).thenComparing(event -> event.name().toLowerCase(Locale.ROOT)));
        return events;
    }

    private static NextEvent computeNext(ZonedDateTime now, List<ScheduledEvent> events) {
        LocalDate today = now.toLocalDate();

        for (ScheduledEvent event : events) {
            ZonedDateTime candidate = ZonedDateTime.of(today, event.time(), EVENT_ZONE);
            if (candidate.isAfter(now)) {
                return new NextEvent(event.name(), candidate);
            }
        }

        ScheduledEvent first = events.getFirst();
        return new NextEvent(first.name(), ZonedDateTime.of(today.plusDays(1L), first.time(), EVENT_ZONE));
    }

    private static Optional<LocalTime> parseTime(String value) {
        String text = safeTrim(value);
        if (!text.matches("\\d{1,2}:\\d{2}")) {
            return Optional.empty();
        }
        String[] split = text.split(":");
        try {
            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);
            return Optional.of(LocalTime.of(hour, minute));
        } catch (NumberFormatException | DateTimeException ignored) {
            return Optional.empty();
        }
    }

    private static void sortStateEvents(List<PersistentState.ScheduledEvent> events) {
        events.sort((a, b) -> {
            Optional<LocalTime> timeA = parseTime(a == null ? "" : a.time);
            Optional<LocalTime> timeB = parseTime(b == null ? "" : b.time);
            if (timeA.isPresent() && timeB.isPresent()) {
                int compare = timeA.get().compareTo(timeB.get());
                if (compare != 0) {
                    return compare;
                }
            } else if (timeA.isPresent()) {
                return -1;
            } else if (timeB.isPresent()) {
                return 1;
            }
            String nameA = safeTrim(a == null ? "" : a.name).toLowerCase(Locale.ROOT);
            String nameB = safeTrim(b == null ? "" : b.name).toLowerCase(Locale.ROOT);
            return nameA.compareTo(nameB);
        });
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private record ScheduledEvent(String name, LocalTime time) {
    }

    private record NextEvent(String name, ZonedDateTime at) {
    }
}
