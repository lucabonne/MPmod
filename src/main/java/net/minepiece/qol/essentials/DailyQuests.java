package net.minepiece.qol.essentials;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Adapted from Aure64's PassQuestParser/State; progress is read from server lore. */
public final class DailyQuests {
    public record Quest(int number, String difficulty, String objective, int current, int target, boolean completed) {
        public boolean done() { return completed || target > 0 && current >= target; }
    }
    private static final Pattern NAME = Pattern.compile("(?:Qu[eê]te|Quest|Misi[oó]n) #(\\d+)\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INT = Pattern.compile("\\d+");
    private static final Pattern COMPLETION = Pattern.compile("^[^\\p{IsLatin}\\p{N}]{0,8}(?:You (?:have )?completed (?:the )?quest|Vous avez complété la quête|Has completado la misión)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private List<Quest> quests = List.of();
    private LocalDate day = LocalDate.now();

    public static Optional<Quest> parse(String name, List<String> lore) {
        var match = NAME.matcher(name);
        if (!match.find()) return Optional.empty();
        try {
            String objective = null;
            int current = 0, target = 0;
            boolean progress = false, completed = false;
            for (String raw : lore) {
                String line = raw.trim(), lower = line.toLowerCase(Locale.ROOT);
                if (objective == null && (line.startsWith("▪") || line.startsWith("•"))) objective = line.substring(1).trim();
                if (lower.contains("progress") || lower.contains("progreso")) progress = true;
                else if (progress) {
                    var nums = INT.matcher(line);
                    List<Integer> values = new ArrayList<>();
                    while (nums.find()) values.add(Integer.parseInt(nums.group()));
                    if (values.size() >= 2) { current = values.getFirst(); target = values.getLast(); progress = false; }
                }
                if (lower.contains("status") || lower.contains("statut") || lower.contains("estado")) {
                    completed = (lower.contains("compl") || lower.contains("termin"))
                        && !lower.contains("non compl") && !lower.contains("not compl") && !lower.contains("no compl") && !lower.contains("incompl");
                }
            }
            return objective == null || objective.isBlank() ? Optional.empty()
                : Optional.of(new Quest(Integer.parseInt(match.group(1)), match.group(2), objective, current, target, completed));
        } catch (NumberFormatException ignored) { return Optional.empty(); }
    }
    public boolean replace(List<Quest> incoming) {
        if (incoming.isEmpty()) return false;
        var sorted = new TreeMap<Integer, Quest>();
        incoming.forEach(q -> sorted.put(q.number(), q));
        quests = List.copyOf(sorted.values());
        return true;
    }
    public void onServerMessage(String message) {
        var match = COMPLETION.matcher(message.trim());
        if (!match.matches()) return;
        String objective = match.group(1).trim();
        quests = quests.stream().map(q -> q.objective().equalsIgnoreCase(objective)
            ? new Quest(q.number(), q.difficulty(), q.objective(), q.target(), q.target(), true) : q).toList();
    }
    public boolean resetDay(LocalDate today) {
        if (today.equals(day)) return false;
        day = today;
        quests = List.of();
        return true;
    }
    public void clear() { quests = List.of(); day = LocalDate.now(); }
    public List<Quest> quests() { return quests; }
    public List<String> lines() {
        return quests.stream().filter(q -> !q.done())
            .map(q -> q.objective() + " (" + q.current() + "/" + q.target() + ")").toList();
    }
}
