package net.minepiece.qol.ui;

import java.util.List;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.UiSettings;

public final class UiLoadouts {
    private UiLoadouts() { }

    public static UiSettings.Loadout capture(MinepieceQolClient mod, String name, int width, int height) {
        UiSettings.Loadout layout = new UiSettings.Loadout();
        layout.name = name.trim();
        layout.screenWidth = Math.max(1, width);
        layout.screenHeight = Math.max(1, height);
        layout.appearance = mod.getConfig().appearance.copy();
        for (int id = 1; id <= 11; id++) {
            layout.panels.put(id, new UiSettings.Placement(mod.getHudPanelX(id), mod.getHudPanelY(id),
                mod.getHudPanelScale(id), mod.getHudPanelColor(id)));
        }
        return layout;
    }

    public static void apply(MinepieceQolClient mod, UiSettings.Loadout layout, int width, int height) {
        mod.getConfig().appearance = layout.appearance.copy();
        for (var entry : layout.panels.entrySet()) {
            int id = entry.getKey();
            var p = entry.getValue();
            if (id < 1 || id > 11 || p == null) continue;
            mod.setHudPanelLayout(id, UiSettings.resizePosition(p.x, layout.screenWidth, width),
                UiSettings.resizePosition(p.y, layout.screenHeight, height), p.scale);
            mod.setHudPanelColor(id, p.color);
        }
        for (var picture : mod.getConfig().appearance.pictures) {
            picture.x = UiSettings.resizePosition(picture.x, layout.screenWidth, width);
            picture.y = UiSettings.resizePosition(picture.y, layout.screenHeight, height);
        }
        mod.getConfig().activeUiLoadout = layout.name;
    }

    public static void preset(MinepieceQolClient mod, String name, int width, int height) {
        List<Integer> priority = switch (name) {
            case "Farming" -> List.of(2, 9, 11, 1);
            case "Fighting" -> List.of(5, 4, 3, 7);
            default -> List.of(10, 9, 2, 5);
        };
        mod.getConfig().appearance.hiddenPanels.clear();
        for (int id = 1; id <= 11; id++) {
            if (!priority.contains(id)) mod.getConfig().appearance.hiddenPanels.add(id);
        }
        // Two panels per edge, leaving the crosshair and central view unobstructed.
        for (int i = 0; i < priority.size(); i++) {
            int x = i < 2 ? 8 : Math.max(8, width - 190);
            int y = i % 2 == 0 ? 8 : Math.max(85, height / 2);
            mod.setHudPanelLayout(priority.get(i), x, y, 0.8F);
        }
        mod.getConfig().activeUiLoadout = name;
    }
}
