package net.minepiece.qol.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minepiece.qol.parse.TooltipParsers;

public final class AuctionHighlighter {
    private static final int MIN_ALPHA = 0x30;
    private static final int MAX_ALPHA = 0xA0;
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

    private static int computeColor(double delta, double intensity) {
        if (Math.abs(delta) < 0.000001D || intensity <= 0.0D) {
            return 0;
        }

        int alpha = MIN_ALPHA + (int) Math.round(intensity * (MAX_ALPHA - MIN_ALPHA));
        alpha = Math.max(MIN_ALPHA, Math.min(MAX_ALPHA, alpha));
        if (delta < 0.0D) {
            return (alpha << 24) | 0x0000FF00;
        }
        return (alpha << 24) | 0x00FF0000;
    }

    public record ActiveHighlight(long sellingPrice, long averagePrice, int quantity, long unitPrice, double delta, int argbColor) {
    }
}
