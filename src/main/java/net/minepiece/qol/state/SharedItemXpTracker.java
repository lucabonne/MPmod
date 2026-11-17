package net.minepiece.qol.state;

import java.util.HashMap;
import java.util.Map;

/** Uses the furthest shared XP counter, never sums the same reward across fruit and weapon updates. */
public final class SharedItemXpTracker {
    public record Progress(int level, long current, long required) { }
    private record Observation(Progress progress, double earned) { }
    private final Map<String, Observation> previous = new HashMap<>();
    private double sharedEarned;

    public void reset() { this.previous.clear(); this.sharedEarned = 0; }

    public void alignSources() {
        this.previous.replaceAll((key, observation) -> new Observation(observation.progress(), this.sharedEarned));
    }

    public double observe(Map<String, Progress> current) {
        this.previous.keySet().retainAll(current.keySet());
        double maximum = this.sharedEarned;
        for (var entry : current.entrySet()) {
            Progress value = entry.getValue();
            Observation old = this.previous.get(entry.getKey());
            double earned = old == null ? this.sharedEarned : old.earned();
            if (old != null) {
                Progress before = old.progress();
                if (value.level() == before.level() && value.current() >= before.current()) {
                    earned += value.current() - before.current();
                } else if (value.level() == before.level() + 1 && before.required() > 0) {
                    earned += before.required() - before.current() + value.current();
                } else {
                    earned = this.sharedEarned; // New item, ascension, or unknown skipped level: rebaseline.
                }
            }
            this.previous.put(entry.getKey(), new Observation(value, earned));
            maximum = Math.max(maximum, earned);
        }
        double gain = maximum - this.sharedEarned;
        this.sharedEarned = maximum;
        return gain;
    }
}
