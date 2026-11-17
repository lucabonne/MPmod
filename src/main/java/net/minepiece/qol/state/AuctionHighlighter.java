package net.minepiece.qol.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minepiece.qol.parse.TooltipParsers;

public final class AuctionHighlighter {
    /** Prices within ±5% of the average are not tinted; each further 10% is one more step. */
    private static final double DEAD_ZONE = 0.05D;
    private static final double STEP = 0.10D;
    public static final int MAX_LEVEL = 10;
    private static final int MIN_ALPHA = 0x99; // drawn behind the item, so it can be near-solid
    private static final int MAX_ALPHA = 0xFF;
    /** Pale → deep green for cheaper, pale yellow → orange → red → crimson for pricier. */
    private static final int[] CHEAP = {0xC6FFB3, 0x2EE62E, 0x00995C};
    private static final int[] PRICEY = {0xFFF2A0, 0xFF9A1F, 0xFF2A2A, 0xB0001E};
    private final Map<String, ActiveHighlight> cachedHighlights = new HashMap<>();

    public void update(String itemKey, TooltipParsers.AuctionParseResult result) {
        if (itemKey == null || itemKey.isBlank()) {
            return;
        }
        createHighlight(result).ifPresent(highlight -> this.cachedHighlights.put(itemKey, highlight));
    }

    public void clear() {
        this.cachedHighlights.clear();
    }

    public Optional<ActiveHighlight> createHighlight(TooltipParsers.AuctionParseResult result) {
        int argbColor = computeColor(result.delta(), result.intensity());
        if (argbColor == 0) {
            return Optional.empty();
        }

        return Optional.of(new ActiveHighlight(
            Math.round(result.sellingPrice()),
            Math.round(result.averagePrice()),
            result.quantity(),
            Math.round(result.unitPrice()),
            result.delta(),
            argbColor
        ));
    }

    public Optional<ActiveHighlight> getCachedHighlight(String itemKey) {
        if (itemKey == null || itemKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.cachedHighlights.get(itemKey));
    }

    private static int computeColor(double delta, double ignoredIntensity) {
        int level = level(delta);
        if (level == 0) {
            return 0;
        }
        int alpha = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * (level - 1) / (MAX_LEVEL - 1);
        return (alpha << 24) | rgbForDelta(delta);
    }

    /** 0 inside ±5%, then 1 for 5–15%, 2 for 15–25% … up to 10 at 95% and beyond. */
    public static int level(double delta) {
        double away = Math.abs(delta);
        if (!(away >= DEAD_ZONE)) {
            return 0;
        }
        return Math.min(MAX_LEVEL, 1 + (int) Math.floor((away - DEAD_ZONE) / STEP + 1e-9));
    }

    /** Color of this delta's step (only meaningful when {@link #level} is above 0). */
    public static int rgbForDelta(double delta) {
        int[] stops = delta < 0 ? CHEAP : PRICEY;
        double position = (Math.max(1, level(delta)) - 1) / (double) (MAX_LEVEL - 1) * (stops.length - 1);
        int from = Math.min(stops.length - 2, (int) position);
        return mix(stops[from], stops[from + 1], position - from);
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = (int) Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    public record ActiveHighlight(long sellingPrice, long averagePrice, int quantity, long unitPrice, double delta, int argbColor) {
    }
}
