package net.minepiece.qol.state;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public final class RarityDetector {
    public enum Rarity {
        MYTHIC,
        LEGENDARY,
        EPIC,
        RARE,
        COMMON
    }

    private static final Pattern NON_LETTER_SPLIT = Pattern.compile("[^A-Z]+");
    private static final Pattern NON_LETTERS = Pattern.compile("[^A-Z]");
    private static final String HIDDEN_LEGENDARY_MARKER = "伴叹";
    private static final String HIDDEN_MYTHIC_MARKER = "愈潮";
    private static final String HIDDEN_RARE_MARKER = "桥淡";
    private static final String HIDDEN_EPIC_MARKER = "恨繁";
    private static final String HIDDEN_COMMON_MARKER = "孔宜";

    private RarityDetector() {
    }

    public static Optional<Rarity> detect(ItemStack stack) {
        return detect(stack, null);
    }

    public static Optional<Rarity> detect(ItemStack stack, @Nullable List<Text> tooltipLines) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        // Primary path: use the actual tooltip text visible in GUI.
        if (tooltipLines != null) {
            return Optional.ofNullable(findSingleRarity(tooltipLines));
        }

        // Fallback path for callers that don't have tooltip lines.
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            return Optional.ofNullable(findSingleRarity(lore.lines()));
        }
        return Optional.empty();
    }

    private static Rarity findSingleRarity(List<Text> lines) {
        Rarity caretPriority = null;
        Rarity standalonePriority = null;
        Rarity uniqueAcrossAll = null;
        boolean multipleAcrossAll = false;

        for (Text line : lines) {
            if (line == null) {
                continue;
            }

            String raw = line.getString();
            LineRarity lineRarity = extractLineRarity(raw);
            if (lineRarity == null) {
                continue;
            }

            if (lineRarity.hasCaret()) {
                if (caretPriority != null && caretPriority != lineRarity.rarity()) {
                    return null;
                }
                caretPriority = lineRarity.rarity();
                continue;
            }

            if (lineRarity.isStandalone()) {
                if (standalonePriority != null && standalonePriority != lineRarity.rarity()) {
                    return null;
                }
                standalonePriority = lineRarity.rarity();
            }

            if (!multipleAcrossAll) {
                if (uniqueAcrossAll == null) {
                    uniqueAcrossAll = lineRarity.rarity();
                } else if (uniqueAcrossAll != lineRarity.rarity()) {
                    multipleAcrossAll = true;
                }
            }
        }

        if (caretPriority != null) {
            return caretPriority;
        }
        if (standalonePriority != null) {
            return standalonePriority;
        }
        return multipleAcrossAll ? null : uniqueAcrossAll;
    }

    private static LineRarity extractLineRarity(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        Rarity hiddenMarkerRarity = extractHiddenMarkerRarity(raw);
        boolean hasHiddenMarker = hiddenMarkerRarity != null;
        Rarity found = hiddenMarkerRarity;
        String[] tokens = NON_LETTER_SPLIT.split(raw.toUpperCase());
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            Rarity rarity = mapToken(token);
            if (rarity == null) {
                continue;
            }
            if (found != null && found != rarity) {
                return null;
            }
            found = rarity;
        }
        if (found == null) {
            return null;
        }

        String collapsed = NON_LETTERS.matcher(raw.toUpperCase()).replaceAll("");
        boolean standalone = hasHiddenMarker || isStandaloneCollapsed(collapsed, found);
        boolean hasCaret = hasHiddenMarker || raw.contains("^");
        return new LineRarity(found, standalone, hasCaret);
    }

    private static Rarity extractHiddenMarkerRarity(String raw) {
        Rarity found = null;
        int matches = 0;
        if (raw.contains(HIDDEN_MYTHIC_MARKER)) {
            found = Rarity.MYTHIC;
            matches++;
        }
        if (raw.contains(HIDDEN_LEGENDARY_MARKER)) {
            found = found == null ? Rarity.LEGENDARY : found;
            if (found != Rarity.LEGENDARY) {
                return null;
            }
            matches++;
        }
        if (raw.contains(HIDDEN_EPIC_MARKER)) {
            found = found == null ? Rarity.EPIC : found;
            if (found != Rarity.EPIC) {
                return null;
            }
            matches++;
        }
        if (raw.contains(HIDDEN_RARE_MARKER)) {
            found = found == null ? Rarity.RARE : found;
            if (found != Rarity.RARE) {
                return null;
            }
            matches++;
        }
        if (raw.contains(HIDDEN_COMMON_MARKER)) {
            found = found == null ? Rarity.COMMON : found;
            if (found != Rarity.COMMON) {
                return null;
            }
            matches++;
        }
        if (matches > 1) {
            return null;
        }
        return found;
    }

    private static Rarity mapToken(String token) {
        return switch (token) {
            case "MYTHIC", "MYTHICAL" -> Rarity.MYTHIC;
            case "LEGENDARY" -> Rarity.LEGENDARY;
            case "EPIC" -> Rarity.EPIC;
            case "RARE" -> Rarity.RARE;
            case "COMMON" -> Rarity.COMMON;
            default -> null;
        };
    }

    private static boolean isStandaloneCollapsed(String collapsed, Rarity rarity) {
        return switch (rarity) {
            case MYTHIC -> "MYTHIC".equals(collapsed) || "MYTHICAL".equals(collapsed);
            case LEGENDARY -> "LEGENDARY".equals(collapsed);
            case EPIC -> "EPIC".equals(collapsed);
            case RARE -> "RARE".equals(collapsed);
            case COMMON -> "COMMON".equals(collapsed);
        };
    }

    private record LineRarity(Rarity rarity, boolean isStandalone, boolean hasCaret) {
    }
}
