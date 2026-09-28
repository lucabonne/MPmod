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
}
