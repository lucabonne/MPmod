package net.minepiece.qol.ui;

import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.config.UiSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiLoadoutsTest {
    private MinepieceQolClient client() throws Exception {
        MinepieceQolClient client = new MinepieceQolClient();
        var field = MinepieceQolClient.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(client, new ConfigManager.ModConfig());
        return client;
    }

    @Test void saveEditRestoreIncludesImagesAndDoesNotChangeTracking() throws Exception {
        var mod = client();
        mod.getConfig().moneyTrackingEnabled = false;
        mod.setHudPanelLayout(9, 120, 60, 0.75F);
        mod.setHudPanelColor(9, "red");
        mod.getConfig().appearance.hiddenPanels.add(4);
        var picture = new UiSettings.Picture();
        picture.file = "cat.png"; picture.x = 80; picture.y = 40;
        mod.getConfig().appearance.pictures.add(picture);
        var saved = UiLoadouts.capture(mod, "Cats", 800, 600);
        mod.setHudPanelLayout(9, 400, 300, 1.5F);
        mod.getConfig().appearance.pictures.getFirst().x = 500;
        UiLoadouts.apply(mod, saved, 400, 300);
        assertEquals(60, mod.getHudPanelX(9));
        assertEquals(30, mod.getHudPanelY(9));
        assertEquals(0.75F, mod.getHudPanelScale(9));
        assertEquals("red", mod.getHudPanelColor(9));
        assertEquals(40, mod.getHudPanelX(12));
        assertFalse(mod.isHudPanelVisible(4));
        assertFalse(mod.getConfig().moneyTrackingEnabled);
        mod.getConfig().appearance.pictures.getFirst().x = 900;
        assertEquals(80, saved.appearance.pictures.getFirst().x);
    }

    @Test void presetsPrioritizeDifferentPanelsAndKeepStoredLayoutUnchanged() throws Exception {
        var mod = client();
        var original = UiLoadouts.capture(mod, "Original", 640, 360);
        UiLoadouts.preset(mod, "Farming", 640, 360);
        assertTrue(mod.isHudPanelVisible(11));
        assertFalse(mod.isHudPanelVisible(3));
        UiLoadouts.preset(mod, "Fighting", 640, 360);
        assertTrue(mod.isHudPanelVisible(3));
        assertTrue(mod.isHudPanelVisible(7));
        assertFalse(mod.isHudPanelVisible(10));
        UiLoadouts.preset(mod, "Grinding", 640, 360);
        assertTrue(mod.isHudPanelVisible(10));
        assertTrue(mod.isHudPanelVisible(9));
        assertTrue(mod.getConfig().cookingHudEnabled);
        UiLoadouts.apply(mod, original, 640, 360);
        assertTrue(mod.isHudPanelVisible(3));
        assertEquals(1, mod.getHudPanelScale(9));
        assertTrue(original.appearance.hiddenPanels.isEmpty());
    }
}
