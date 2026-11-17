package net.minepiece.qol.ui;

import java.util.List;
import java.util.random.RandomGenerator;

final class PictureRotation {
    private final RandomGenerator random;
    private List<String> files = List.of();
    private int index;
    private int intervalSeconds;
    private long changedAt;

    PictureRotation(RandomGenerator random) {
        this.random = random;
    }

    String current(List<String> nextFiles, int seconds, long nowNanos) {
        int interval = Math.clamp(seconds, 1, 3600);
        if (!files.equals(nextFiles)) {
            files = List.copyOf(nextFiles);
            index = files.isEmpty() ? 0 : random.nextInt(files.size());
            intervalSeconds = interval;
            changedAt = nowNanos;
        } else if (intervalSeconds != interval) {
            intervalSeconds = interval;
            changedAt = nowNanos;
        } else if (files.size() > 1 && nowNanos - changedAt >= interval * 1_000_000_000L) {
            int next = random.nextInt(files.size() - 1);
            index = next >= index ? next + 1 : next;
            changedAt = nowNanos;
        }
        return files.isEmpty() ? null : files.get(index);
    }
}
