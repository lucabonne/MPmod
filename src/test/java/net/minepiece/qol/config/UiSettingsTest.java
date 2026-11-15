package net.minepiece.qol.config;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Test void legacyPicturesKeepTheirSettingsWithoutEnablingRotation() {
        var settings = new Gson().fromJson("{\"pictures\":[{\"file\":\"cat.png\",\"x\":145,\"opacity\":60}]}", UiSettings.class);
        settings.normalize();
        var picture = settings.pictures.getFirst();
        assertEquals("cat.png", picture.file);
        assertEquals(145, picture.x);
        assertEquals(60, picture.opacity);
        assertTrue(picture.rotationFiles.isEmpty());
        assertEquals(10, picture.intervalSeconds);
        assertFalse(picture.fullscreen);
        assertEquals("none", picture.fade);
        assertEquals(50, picture.fadePercent);
    }

    @Test void normalizesFullscreenAndFadeSettings() {
        var settings = new Gson().fromJson("{\"pictures\":[{\"file\":\"cat.png\",\"fullscreen\":true,\"fade\":\"diagonal\",\"fadePercent\":900},"
            + "{\"file\":\"dog.png\",\"fade\":\"bottom\",\"fadePercent\":0}]}", UiSettings.class);
        settings.normalize();
        assertTrue(settings.pictures.get(0).fullscreen);
        assertEquals("none", settings.pictures.get(0).fade);
        assertEquals(100, settings.pictures.get(0).fadePercent);
        assertEquals("bottom", settings.pictures.get(1).fade);
        assertEquals(5, settings.pictures.get(1).fadePercent);
    }

    @Test void rotationSettingsSurviveCopyAndSavedLoadoutRoundTripIndependently() {
        UiSettings settings = new UiSettings();
        var picture = new UiSettings.Picture();
        picture.file = "cat.png";
        picture.rotationFiles.addAll(List.of("cat.png", "dog.png"));
        picture.intervalSeconds = 42;
        settings.pictures.add(picture);
        var saved = new UiSettings.Loadout();
        saved.appearance = settings.copy();
        picture.rotationFiles.clear();
        picture.intervalSeconds = 1;
        var decoded = new Gson().fromJson(new Gson().toJson(saved), UiSettings.Loadout.class);
        assertEquals(List.of("cat.png", "dog.png"), decoded.appearance.pictures.getFirst().rotationFiles);
        assertEquals(42, decoded.appearance.pictures.getFirst().intervalSeconds);
        assertEquals(List.of("cat.png", "dog.png"), saved.appearance.pictures.getFirst().rotationFiles);
    }

    @Test void normalizesRotationFilesAndIntervalWithoutRemovingValidPictures() {
        UiSettings settings = new UiSettings();
        var picture = new UiSettings.Picture();
        picture.file = "cat.png";
        picture.rotationFiles = new ArrayList<>(Arrays.asList("cat.png", null, "../secret.png", "dog.png", "cat.png", ""));
        picture.intervalSeconds = 0;
        settings.pictures.add(picture);
        settings.normalize();
        assertEquals(1, settings.pictures.size());
        assertEquals(List.of("cat.png", "dog.png"), picture.rotationFiles);
        assertEquals(1, picture.intervalSeconds);
        picture.rotationFiles = null;
        picture.intervalSeconds = Integer.MAX_VALUE;
        settings.normalize();
        assertTrue(picture.rotationFiles.isEmpty());
        assertEquals(3600, picture.intervalSeconds);
    }

    @Test void pictureZoomGoesPastOldLimitUpToScreenCover() {
        var picture = new UiSettings.Picture();
        picture.width = 80; picture.height = 40;
        assertEquals(10F, UiSettings.coverScale(picture, 800, 300)); // width decides
        assertEquals(15F, UiSettings.coverScale(picture, 400, 600)); // height decides
        var settings = new UiSettings();
        picture.file = "cat.png"; picture.scale = 12F;
        settings.pictures.add(picture);
        settings.normalize();
        assertEquals(12F, picture.scale); // no longer clamped to 1.8
    }
}
