package net.minepiece.qol.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minepiece.qol.parse.TooltipParsers;

public final class AuctionTooltipFormatter {
    private static final Pattern PRICE_SUFFIX_PATTERN =
        Pattern.compile("\\s\\([0-9]+(?:\\.[0-9]+)?[KM]?/[^()]+\\)(?:\\s[+-]?[0-9]+(?:\\.[0-9]+)?%)?\\s*$");

    private AuctionTooltipFormatter() {
    }

    public static void appendPrices(List<Component> lines, List<String> normalizedLines,
                                    TooltipParsers.AuctionParseResult result, String perItemLabel) {
        for (int i = 0; i < normalizedLines.size(); i++) {
            String line = normalizedLines.get(i);
            boolean selling = TooltipParsers.isAuctionSellingPriceLine(line);
            if (!selling && !TooltipParsers.isAuctionAveragePriceLine(line)) {
                continue;
            }
            if (PRICE_SUFFIX_PATTERN.matcher(lines.get(i).getString()).find()) {
                continue;
            }

            double unitPrice = selling ? result.unitPrice() : result.averageUnitPrice();
            MutableComponent updated = lines.get(i).copy().append(
                Component.literal(" (" + formatPrice(unitPrice) + perItemLabel + ")").withStyle(ChatFormatting.GRAY)
            );
            if (selling) {
                updated.append(Component.literal(" " + formatDeltaPercent(result.delta()))
                    .withStyle(deltaColor(result.delta())));
            }
            lines.set(i, updated);
        }
    }

    static String formatPrice(double value) {
        BigDecimal price = BigDecimal.valueOf(value);
        String suffix = "";
        if (value >= 1_000_000D) {
            price = price.movePointLeft(6);
            suffix = "M";
        } else if (value >= 1_000D) {
            price = price.movePointLeft(3);
            suffix = "K";
            if (price.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(1000)) >= 0) {
                price = price.movePointLeft(3);
                suffix = "M";
            }
        }
        return price.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + suffix;
    }

    static String formatDeltaPercent(double delta) {
        BigDecimal percent = BigDecimal.valueOf(delta).movePointRight(2)
            .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return (percent.signum() > 0 ? "+" : "") + percent.toPlainString() + "%";
    }

    static ChatFormatting deltaColor(double delta) {
        if (delta <= -0.90D) {
            return ChatFormatting.YELLOW;
        }
        if (delta <= -0.74D) {
            return ChatFormatting.BLUE;
        }
        if (delta <= -0.50D) {
            return ChatFormatting.LIGHT_PURPLE;
        }
        if (delta < 0.0D) {
            return ChatFormatting.GREEN;
        }
        if (delta > 0.0D) {
            return ChatFormatting.RED;
        }
        return ChatFormatting.GRAY;
    }
}
