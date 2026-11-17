package net.minepiece.qol.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;
import net.minepiece.qol.i18n.UiLocalization;
import org.junit.jupiter.api.Test;

class HudValueColorsTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final Function<String, String> EN = key -> UiLocalization.text("en", key);
    private static final Function<String, String> FR = key -> UiLocalization.text("fr", key);

    private static int color(String line, Function<String, String> tr) {
        String[] parts = IconGridTooltipComponent.splitValue(line);
        return HudValueColors.color(parts[0], parts[1], tr, WHITE);
    }

    @Test void moneyInIsGreenOutIsRedZeroAndUnknownAreGrey() {
        assertEquals(HudValueColors.GREEN, color("Made today: 84.2K", EN));
        assertEquals(HudValueColors.GREEN, color("AH sold: 31.0K", EN));
        assertEquals(HudValueColors.RED, color("AH bought: 12.5K", EN));
        assertEquals(HudValueColors.DIM, color("AH bought: 0", EN));
        assertEquals(HudValueColors.DIM, color("Made today: ?/?", EN));
        assertEquals(WHITE, color("Total: 2.41M", EN));
    }

    @Test void grindingSafesAndSigns() {
        assertEquals(HudValueColors.GREEN, color(EN.apply("grinding.active"), EN));
        assertEquals(HudValueColors.AMBER, color(EN.apply("grinding.paused"), EN));
        assertEquals(HudValueColors.GREEN, color("XP/h: ~12.6K", EN));
        assertEquals(HudValueColors.DIM, color("Money/h: —", EN));
        assertEquals(HudValueColors.DIM, color("XP gained: ~0", EN));
        assertEquals(WHITE, color("Active time: 14m 2s", EN));
        assertEquals(HudValueColors.GREEN, color("safes.green: Ready", EN));
        assertEquals(HudValueColors.DIM, color("Red: Picked", EN));
        assertEquals(WHITE, color("Purple: 1m 50s", EN));
        assertEquals(HudValueColors.GREEN, color("Balance: +500", EN));
        assertEquals(HudValueColors.RED, color("Balance: -500", EN));
        assertEquals(WHITE, color("Power: 1,240", EN));
    }

    @Test void worksInOtherLanguages() {
        assertEquals(HudValueColors.RED, color(FR.apply("money.ah_bought") + " 12.5K", FR));
        assertEquals(HudValueColors.GREEN, color(FR.apply("money.made_today") + " 1K", FR));
    }
}
