package net.minepiece.qol.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CustomPicturesTest {
    @TempDir Path temp;

    @Test void importsResizesAndPreservesTransparencyWithoutChangingOriginal() throws Exception {
        var image = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(2, 2, 0xFFFFAABB);
        Path original = temp.resolve("Teddy cat.png");
        ImageIO.write(image, "png", original.toFile());
        byte[] before = Files.readAllBytes(original);
        var picture = CustomPictures.importPicture(original, temp.resolve("imports"));
        var saved = ImageIO.read(temp.resolve("imports").resolve(picture.file).toFile());
        assertEquals(512, saved.getWidth());
        assertEquals(256, saved.getHeight());
        assertEquals(80, picture.width);
        assertEquals(40, picture.height);
        assertEquals(0, saved.getRGB(0, 0) >>> 24);
        assertArrayEquals(before, Files.readAllBytes(original));
        assertEquals("Teddy cat.png", picture.name);
    }

    @Test void acceptsJpgAndUsesDistinctNamesForDuplicateImports() throws Exception {
        Path source = temp.resolve("cat.jpg");
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "jpg", source.toFile());
        var first = CustomPictures.importPicture(source, temp.resolve("imports"));
        var second = CustomPictures.importPicture(source, temp.resolve("imports"));
        assertNotEquals(first.file, second.file);
        assertTrue(first.file.endsWith(".png"));
    }

    @Test void rejectsInvalidAndOversizedImages() throws Exception {
        Path text = temp.resolve("fake.png");
        Files.writeString(text, "not an image");
        assertThrows(IOException.class, () -> CustomPictures.importPicture(text, temp));
        assertThrows(IOException.class, () -> CustomPictures.importPicture(temp.resolve("missing.png"), temp));
        Path wide = temp.resolve("wide.png");
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_ARGB), "png", wide.toFile());
        assertThrows(IOException.class, () -> CustomPictures.importPicture(wide, temp));
    }

    @Test void importsFolderAsOneRotationAndSkipsUnsupportedOrInvalidFiles() throws Exception {
        Path folder = Files.createDirectory(temp.resolve("Holiday photos"));
        var image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
        Path png = folder.resolve("one.PNG");
        Path jpg = folder.resolve("two.jpg");
        Path jpeg = folder.resolve("three.JPEG");
        ImageIO.write(image, "png", png.toFile());
        ImageIO.write(image, "jpg", jpg.toFile());
        ImageIO.write(image, "jpg", jpeg.toFile());
        byte[] pngBefore = Files.readAllBytes(png);
        byte[] jpgBefore = Files.readAllBytes(jpg);
        byte[] jpegBefore = Files.readAllBytes(jpeg);
        Files.writeString(folder.resolve("broken.png"), "not an image");
        Files.writeString(folder.resolve("notes.txt"), "keep me");
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", folder.resolve("wide.png").toFile());
        Path nested = Files.createDirectory(folder.resolve("nested"));
        ImageIO.write(image, "png", nested.resolve("hidden.png").toFile());

        Path imports = temp.resolve("imports");
        var picture = CustomPictures.importFolder(folder, imports);

        assertEquals("Holiday photos", picture.name);
        assertEquals(3, picture.rotationFiles.size());
        assertEquals(3, picture.rotationFiles.stream().distinct().count());
        assertEquals(picture.file, picture.rotationFiles.getFirst());
        assertEquals(10, picture.intervalSeconds);
        for (String file : picture.rotationFiles) {
            assertTrue(file.matches("[a-zA-Z0-9-]+\\.png"));
            assertNotNull(ImageIO.read(imports.resolve(file).toFile()));
        }
        assertArrayEquals(pngBefore, Files.readAllBytes(png));
        assertArrayEquals(jpgBefore, Files.readAllBytes(jpg));
        assertArrayEquals(jpegBefore, Files.readAllBytes(jpeg));
        assertEquals("keep me", Files.readString(folder.resolve("notes.txt")));
    }

    @Test void rejectsFoldersWithoutAnyUsableImages() throws Exception {
        Path folder = Files.createDirectory(temp.resolve("empty"));
        assertThrows(IOException.class, () -> CustomPictures.importFolder(folder, temp.resolve("imports")));
        Files.writeString(folder.resolve("bad.jpg"), "not an image");
        assertThrows(IOException.class, () -> CustomPictures.importFolder(folder, temp.resolve("imports")));
        assertThrows(IOException.class, () -> CustomPictures.importFolder(temp.resolve("missing"), temp.resolve("imports")));
        assertThrows(IOException.class, () -> CustomPictures.importFolder(folder.resolve("bad.jpg"), temp.resolve("imports")));
    }

    @Test void importsASingleImageFolder() throws Exception {
        Path folder = Files.createDirectory(temp.resolve("one photo"));
        ImageIO.write(new BufferedImage(32, 64, BufferedImage.TYPE_INT_ARGB), "png", folder.resolve("one.png").toFile());
        var picture = CustomPictures.importFolder(folder, temp.resolve("imports"));
        assertEquals(1, picture.rotationFiles.size());
        assertEquals(picture.file, picture.rotationFiles.getFirst());
        assertTrue(Files.isRegularFile(temp.resolve("imports").resolve(picture.file)));
    }

    @Test void fadeReachesZeroAtTheChosenEdgeAndFullOpacityPastItsLength() {
        assertEquals(80, CustomPictures.fadeOpacity(80, "none", 50, 0.99));
        assertEquals(0, CustomPictures.fadeOpacity(80, "bottom", 50, 1.0));
        assertEquals(40, CustomPictures.fadeOpacity(80, "bottom", 50, 0.75)); // halfway through the fade
        assertEquals(80, CustomPictures.fadeOpacity(80, "bottom", 50, 0.4));  // before the fade starts
        assertEquals(0, CustomPictures.fadeOpacity(100, "left", 100, 0.0));
        assertEquals(50, CustomPictures.fadeOpacity(100, "left", 100, 0.5));
        assertEquals(100, CustomPictures.fadeOpacity(100, "top", 20, 0.3));
    }
}
