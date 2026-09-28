package net.minepiece.qol.ui;

import com.google.gson.Gson;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.UiSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanelColorTest {
    @Test void acceptsHexWithOptionalHashAndPreservesLegacyNames() {
        assertEquals("#A1B2C3", MinepieceQolClient.normalizeHudColorName("a1b2c3"));
        assertEquals("#A1B2C3", MinepieceQolClient.normalizeHudColorName(" #a1b2c3 "));
        assertEquals("red", MinepieceQolClient.normalizeHudColorName("RED"));
        assertEquals("default", MinepieceQolClient.normalizeHudColorName("#xyz123"));
        assertEquals("default", MinepieceQolClient.normalizeHudColorName("#12345"));
    }

    @Test void exactHexAndOpacitySurviveLoadoutRoundTrip() {
        UiSettings.Loadout loadout = new UiSettings.Loadout();
        loadout.appearance.hudOpacity = 0;
        loadout.appearance.chatPanelColor = "#A1B2C3";
        loadout.panels.put(10, new UiSettings.Placement(1, 2, 1, "#00FF80"));
        var saved = new Gson().fromJson(new Gson().toJson(loadout), UiSettings.Loadout.class);
        assertEquals(0x00A1B2C3, HudOverlay.panelBackground(saved.appearance.chatPanelColor, saved.appearance));
        saved.appearance.hudOpacity = 100;
        assertEquals(0xFF00FF80, HudOverlay.panelBackground(saved.panels.get(10).color, saved.appearance));
        assertEquals(0xFF000000, HudOverlay.panelBackground("000000", saved.appearance));
        assertEquals(0xFFFFFFFF, HudOverlay.panelBackground("FFFFFF", saved.appearance));
        assertNotEquals(HudOverlay.panelBackground("red", saved.appearance), HudOverlay.panelBackground("default", saved.appearance));
    }
}
