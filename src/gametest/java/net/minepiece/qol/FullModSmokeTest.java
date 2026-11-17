package net.minepiece.qol;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minepiece.qol.config.UiSettings;
import net.minepiece.qol.ui.AppearanceScreen;
import net.minepiece.qol.ui.CustomPictures;
import net.minepiece.qol.ui.MinepieceMenuScreen;
import net.minepiece.qol.ui.ThemedButton;

public final class FullModSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                var mod = MinepieceQolClient.get();
                if (mod == null || mod.getConfig() == null || mod.getEssentials() == null) {
                    throw new AssertionError("Full mod did not initialize");
                }
                for (String name : new String[]{"open_menu", "hud_editor", "loadout"}) {
                    if (KeyMapping.get("key.minepiece-qol." + name) == null) {
                        throw new AssertionError("Missing full-mod keybinding: " + name);
                    }
                }
                client.gui.setScreen(new MinepieceMenuScreen(mod));
            });
            context.waitTicks(10);
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof MinepieceMenuScreen)) {
                    throw new AssertionError("Full mod menu did not stay open");
                }
            });
            context.takeScreenshot("2.0.5-full-menu");
            verifyPictures(context);
            context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
            context.waitTicks(10);
            context.takeScreenshot("2.0.5-inventory");
            context.runOnClient(client -> client.gui.setScreen(null));
            context.waitTicks(5);
            context.takeScreenshot("2.0.5-hud");
            context.runOnClient(client -> client.gui.setScreen(new net.minepiece.qol.ui.HudLayoutScreen(MinepieceQolClient.get())));
            context.waitTicks(10);
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof net.minepiece.qol.ui.HudLayoutScreen)) throw new AssertionError("HUD editor did not open");
            });
            context.takeScreenshot("2.1.4-hud-editor");
            context.runOnClient(client -> ((net.minepiece.qol.ui.HudLayoutScreen) client.gui.screen()).autoArrange());
            context.waitTicks(5);
            context.takeScreenshot("2.1.4-auto-arranged");
            context.runOnClient(client -> client.gui.setScreen(null));
            context.waitTicks(5);
        }
    }

    private void verifyPictures(ClientGameTestContext context) {
        try {
            Path folder = Files.createTempDirectory("image-rotation-smoke-");
            var originals = new ArrayList<UiSettings.Picture>();
            var importedFiles = new HashSet<String>();
            context.runOnClient(client -> {
                var pictures = MinepieceQolClient.get().getConfig().appearance.pictures;
                originals.addAll(pictures);
                pictures.clear();
            });
            try {
                int[] colors = {0xFFE95B5B, 0xFF45B8CE};
                for (int i = 0; i < colors.length; i++) {
                    var image = new BufferedImage(96, 64, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, colors[i]);
                    }
                    ImageIO.write(image, "png", folder.resolve("image-" + i + ".png").toFile());
                }
                Files.writeString(folder.resolve("ignored.txt"), "This is not an image.");
                context.runOnClient(client -> {
                    var mod = MinepieceQolClient.get();
                    var screen = new AppearanceScreen(mod, client.gui.screen(), 2);
                    client.gui.setScreen(screen);
                    screen.onFilesDrop(List.of(folder));
                    var pictures = mod.getConfig().appearance.pictures;
                    for (var picture : pictures) importedFiles.addAll(picture.rotationFiles);
                    if (pictures.size() != 1 || pictures.getFirst().rotationFiles.size() != 2
                            || pictures.getFirst().intervalSeconds != 10) {
                        throw new AssertionError("Folder did not import as one image rotation");
                    }
                    var folderButton = screen.children().stream().filter(ThemedButton.class::isInstance)
                        .map(ThemedButton.class::cast)
                        .filter(button -> button.getMessage().getString().equals(mod.tr("ui.choose_folder")))
                        .findFirst().orElseThrow(() -> new AssertionError("Missing folder control"));
                    var interval = screen.children().stream().filter(EditBox.class::isInstance)
                        .map(EditBox.class::cast)
                        .filter(field -> field.getMessage().getString().equals(mod.tr("ui.rotation_interval")))
                        .findFirst().orElseThrow(() -> new AssertionError("Missing interval control"));
                    if (!folderButton.visible || !interval.visible) throw new AssertionError("Hidden image controls");
                    interval.setValue("0");
                    if (pictures.getFirst().intervalSeconds != 10) throw new AssertionError("Invalid interval accepted");
                    interval.setValue("2");
                    if (pictures.getFirst().intervalSeconds != 2) throw new AssertionError("Interval edit did not apply");
                    for (String label : List.of("+", "−")) {
                        var button = screen.children().stream().filter(ThemedButton.class::isInstance)
                            .map(ThemedButton.class::cast)
                            .filter(widget -> widget.getY() == interval.getY() && widget.getMessage().getString().equals(label))
                            .findFirst().orElseThrow(() -> new AssertionError("Missing interval button: " + label));
                        button.onClick(new MouseButtonEvent(button.getX() + 1, button.getY() + 1,
                            new MouseButtonInfo(0, 0)), false);
                        int expected = label.equals("+") ? 3 : 2;
                        if (pictures.getFirst().intervalSeconds != expected) throw new AssertionError("Interval button failed: " + label);
                    }
                });
                context.waitTicks(10);
                context.takeScreenshot("2.0.5-images");
                context.runOnClient(client -> {
                    var mod = MinepieceQolClient.get();
                    var picture = mod.getConfig().appearance.pictures.getFirst();
                    picture.fullscreen = true;
                    picture.fade = "bottom";
                    picture.fadePercent = 60;
                    client.gui.setScreen(null);
                });
                context.waitTicks(10);
                context.takeScreenshot("2.0.6-fullscreen-fade");
            } finally {
                context.runOnClient(client -> {
                    var mod = MinepieceQolClient.get();
                    client.gui.setScreen(null);
                    mod.getCustomPictures().clear();
                    mod.getConfig().appearance.pictures.clear();
                    mod.getConfig().appearance.pictures.addAll(originals);
                    mod.saveConfig();
                });
                for (String file : importedFiles) Files.deleteIfExists(CustomPictures.directory().resolve(file));
                try (var files = Files.list(folder)) {
                    for (Path file : files.toList()) Files.deleteIfExists(file);
                }
                Files.deleteIfExists(folder);
            }
        } catch (IOException error) {
            throw new AssertionError("Could not prepare image rotation fixtures", error);
        }
    }
}
