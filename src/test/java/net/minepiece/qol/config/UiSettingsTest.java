package net.minepiece.qol.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiSettingsTest {
    @Test void customTextOverridesHudStatusColorsAndDefaultRestoresThem() {
        UiSettings settings = new UiSettings();
        assertEquals(0xFFFF0000, settings.hudTextColor(0xFFFF0000));
        settings.text = "00FFCC";
        assertEquals(0xFF00FFCC, settings.hudTextColor(0xFFFF0000));
        assertEquals(0xFF00FFCC, settings.hudTextColor(0xFFFFFFFF));
        settings.text = "EAF1FF";
        assertEquals(0xFFFF0000, settings.hudTextColor(0xFFFF0000));
    }

    @Test void hudStyleAndChatPanelColorSurviveLoadoutCopy() {
        UiSettings settings = new UiSettings();
        settings.chatPanelColor = "purple";
        settings.hudOpacity = 35;
        settings.text = "00FFCC";
        UiSettings copy = settings.copy();
        settings.chatPanelColor = "red";
        settings.hudOpacity = 0;
        assertEquals("purple", copy.chatPanelColor);
        assertEquals(35, copy.hudOpacity);
        assertEquals(0xFF00FFCC, copy.hudTextColor(0xFFFFFFFF));
    }

    @Test void existingConfigGetsCompactDefaultsWithoutLosingOldPositions() {
        var config = new Gson().fromJson("{\"jobsHudX\":123,\"moneyTrackingEnabled\":false}", ConfigManager.ModConfig.class);
        assertEquals(123, config.jobsHudX);
        assertFalse(config.moneyTrackingEnabled);
        assertTrue(config.appearance.compact);
        assertEquals(1, config.appearance.borderWidth);
        assertTrue(config.uiLoadouts.isEmpty());
    }

    @Test void loadoutRoundTripAndCopyAreIndependent() {
        UiSettings appearance = new UiSettings();
        appearance.accent = "FF99AA";
        appearance.hiddenPanels.add(3);
        var picture = new UiSettings.Picture();
        picture.file = "cat.png";
        appearance.pictures.add(picture);
        UiSettings.Loadout saved = new UiSettings.Loadout();
        saved.name = "Teddy's setup";
        saved.appearance = appearance.copy();
        saved.panels.put(9, new UiSettings.Placement(123, 45, 0.8F, "red"));
        appearance.pictures.getFirst().x = 777;
        appearance.hiddenPanels.clear();
        var decoded = new Gson().fromJson(new Gson().toJson(saved), UiSettings.Loadout.class);
        assertEquals("Teddy's setup", decoded.name);
        assertEquals(12, decoded.appearance.pictures.getFirst().x);
        assertEquals(3, decoded.appearance.hiddenPanels.getFirst());
        assertEquals("red", decoded.panels.get(9).color);
        assertEquals(0.8F, decoded.panels.get(9).scale);
    }

    @Test void validatesColorsOpacityAndImportedFileNames() {
        UiSettings settings = new UiSettings();
        settings.background = "not a color";
        settings.hudOpacity = 300;
        settings.borderWidth = -4;
        var picture = new UiSettings.Picture();
        picture.file = "../secret.png";
        settings.pictures.add(picture);
        settings.normalize();
        assertEquals("101723", settings.background);
        assertEquals(100, settings.hudOpacity);
        assertEquals(0, settings.borderWidth);
        assertTrue(settings.pictures.isEmpty());
        assertEquals(0xFF112233, UiSettings.color("112233", 100));
        assertEquals(0x00112233, UiSettings.color("112233", 0));
    }

    @Test void positionsScaleWithResolutionAndPreserveAutomaticPlacement() {
        assertEquals(100, UiSettings.resizePosition(200, 800, 400));
        assertEquals(-1, UiSettings.resizePosition(-1, 800, 400));
        assertEquals(384, UiSettings.resizePosition(800, 800, 400));
    }
}
