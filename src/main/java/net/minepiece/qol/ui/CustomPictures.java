package net.minepiece.qol.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minepiece.qol.config.UiSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class CustomPictures {
    private final Map<String, Identifier> textures = new HashMap<>();
    private final Set<String> failed = new HashSet<>();

    public static Path directory() {
        return FabricLoader.getInstance().getConfigDir().resolve("minepiece-qol/images");
    }

    public static UiSettings.Picture importPicture(Path source, Path directory) throws IOException {
        if (!Files.isRegularFile(source) || Files.size(source) > 10 * 1024 * 1024) {
            throw new IOException("Choose a PNG or JPG smaller than 10 MB.");
        }
        BufferedImage image;
        try (var input = ImageIO.createImageInputStream(source.toFile())) {
            if (input == null) throw new IOException("Cannot read this image.");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Choose a PNG or JPG image.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) {
                    throw new IOException("Choose a PNG or JPG image.");
                }
                if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096) {
                    throw new IOException("Images can be up to 4096 × 4096 pixels.");
                }
                image = reader.read(0);
            } finally {
                reader.dispose();
            }
        }
        double ratio = Math.min(1, 512.0 / Math.max(image.getWidth(), image.getHeight()));
        BufferedImage scaled = new BufferedImage(Math.max(1, (int) (image.getWidth() * ratio)),
            Math.max(1, (int) (image.getHeight() * ratio)), BufferedImage.TYPE_INT_ARGB);
        var graphics = scaled.createGraphics();
        graphics.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        graphics.dispose();
        Files.createDirectories(directory);
        UiSettings.Picture picture = new UiSettings.Picture();
        picture.file = UUID.randomUUID() + ".png";
        picture.name = source.getFileName().toString();
        picture.width = 80;
        picture.height = Math.max(8, (int) Math.round(80.0 * scaled.getHeight() / scaled.getWidth()));
        if (picture.height > 160) {
            picture.width = Math.max(8, 80 * 160 / picture.height);
            picture.height = 160;
        }
        if (!ImageIO.write(scaled, "png", directory.resolve(picture.file).toFile())) {
            throw new IOException("Could not save image.");
        }
        return picture;
    }

    public void draw(DrawContext context, UiSettings.Picture picture, boolean editing) {
        if (!picture.visible && !editing) return;
        int width = Math.max(1, Math.round(picture.width * picture.scale));
        int height = Math.max(1, Math.round(picture.height * picture.scale));
        Identifier texture = textures.get(picture.file);
        if (texture == null && !failed.contains(picture.file)) {
            try {
                if (!picture.file.matches("[a-zA-Z0-9-]+\\.png")) return;
                try (var stream = Files.newInputStream(directory().resolve(picture.file))) {
                    NativeImage nativeImage = NativeImage.read(stream);
                    texture = Identifier.of("minepiece-qol", "custom/" + picture.file);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(texture,
                        new NativeImageBackedTexture(() -> picture.name, nativeImage));
                    textures.put(picture.file, texture);
                }
            } catch (IOException | RuntimeException error) {
                failed.add(picture.file);
            }
        }
        if (texture != null) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, picture.x, picture.y, 0, 0,
                width, height, width, height, UiSettings.color("FFFFFF", picture.opacity));
        } else if (editing) {
            context.fill(picture.x, picture.y, picture.x + width, picture.y + height, 0xAA331111);
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "Image unavailable", picture.x + 2, picture.y + 2, 0xFFFFFFFF);
        }
    }

    public void clear() {
        for (Identifier id : textures.values()) MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        textures.clear();
        failed.clear();
    }
}
