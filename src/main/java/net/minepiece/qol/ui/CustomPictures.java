package net.minepiece.qol.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minepiece.qol.config.UiSettings;

public final class CustomPictures {
    private record PictureTexture(String file, Identifier id, int width, int height) { }
    private final Map<String, PictureTexture> textures = new HashMap<>();
    private final Map<String, PictureTexture> previousTextures = new HashMap<>();
    private final Map<String, PictureRotation> rotations = new HashMap<>();
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

    public static UiSettings.Picture importFolder(Path source, Path directory) throws IOException {
        if (!Files.isDirectory(source)) throw new IOException("Choose a folder of PNG or JPG images.");
        UiSettings.Picture picture = null;
        try (var entries = Files.list(source)) {
            var files = entries.filter(Files::isRegularFile).filter(file -> {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
            }).sorted().toList();
            for (Path file : files) {
                UiSettings.Picture imported;
                try {
                    imported = importPicture(file, directory);
                } catch (IOException | RuntimeException error) {
                    continue;
                }
                if (picture == null) picture = imported;
                picture.rotationFiles.add(imported.file);
            }
        }
        if (picture == null) throw new IOException("No readable PNG or JPG images found (up to 10 MB and 4096 × 4096 each).");
        picture.name = source.getFileName() == null ? source.toString() : source.getFileName().toString();
        return picture;
    }

    public void draw(GuiGraphicsExtractor context, UiSettings.Picture picture, boolean editing) {
        if (!picture.visible && !editing) return;
        int width = Math.max(1, Math.round(picture.width * picture.scale));
        int height = Math.max(1, Math.round(picture.height * picture.scale));
        boolean rotating = picture.rotationFiles != null && !picture.rotationFiles.isEmpty();
        String file = rotating ? rotations.computeIfAbsent(picture.file,
            ignored -> new PictureRotation(ThreadLocalRandom.current()))
            .current(picture.rotationFiles, picture.intervalSeconds, System.nanoTime()) : picture.file;
        PictureTexture texture = textures.get(picture.file);
        if (texture != null && !texture.file.equals(file)) {
            var previous = previousTextures.get(picture.file);
            if (previous != null && previous.file.equals(file)) {
                previousTextures.put(picture.file, texture);
                textures.put(picture.file, previous);
                texture = previous;
            } else {
                texture = null;
            }
        }
        if (texture == null && !failed.contains(file)) {
            try {
                if (!file.matches("[a-zA-Z0-9-]+\\.png")) return;
                try (var stream = Files.newInputStream(directory().resolve(file))) {
                    NativeImage nativeImage = NativeImage.read(stream);
                    Identifier id = Identifier.fromNamespaceAndPath("minepiece-qol", "custom/" + picture.file + "/" + file);
                    texture = new PictureTexture(file, id, nativeImage.getWidth(), nativeImage.getHeight());
                    Minecraft.getInstance().getTextureManager().register(id,
                        new DynamicTexture(() -> picture.name, nativeImage));
                    var old = textures.put(picture.file, texture);
                    var expired = previousTextures.remove(picture.file);
                    if (expired != null) Minecraft.getInstance().getTextureManager().release(expired.id);
                    // Keep the previous image alive for draws already extracted in this frame.
                    if (old != null) previousTextures.put(picture.file, old);
                }
            } catch (IOException | RuntimeException error) {
                failed.add(file);
                texture = null;
            }
        }
        if (texture != null) {
            if (picture.fullscreen) {
                // Cover: scale until both sides fill the screen, then crop the overflow evenly.
                int screenW = context.guiWidth(), screenH = context.guiHeight();
                double cover = Math.max(screenW / (double) texture.width, screenH / (double) texture.height);
                int virtualW = Math.max(screenW, (int) Math.ceil(texture.width * cover));
                int virtualH = Math.max(screenH, (int) Math.ceil(texture.height * cover));
                blitFaded(context, texture.id, picture, 0, 0, screenW, screenH,
                    (virtualW - screenW) / 2, (virtualH - screenH) / 2, virtualW, virtualH);
                return;
            }
            int drawWidth = width;
            int drawHeight = height;
            if (rotating) {
                double fit = Math.min(width / (double) texture.width, height / (double) texture.height);
                drawWidth = Math.max(1, (int) Math.round(texture.width * fit));
                drawHeight = Math.max(1, (int) Math.round(texture.height * fit));
            }
            blitFaded(context, texture.id, picture, picture.x + (width - drawWidth) / 2, picture.y + (height - drawHeight) / 2,
                drawWidth, drawHeight, 0, 0, drawWidth, drawHeight);
        } else if (editing) {
            context.fill(picture.x, picture.y, picture.x + width, picture.y + height, 0xAA331111);
            context.text(Minecraft.getInstance().font, "Image unavailable", picture.x + 2, picture.y + 2, 0xFFFFFFFF);
        }
    }

    private static final int FADE_STRIPS = 48;

    /**
     * Draws a w×h area whose texture is mapped as a virtualW×virtualH image, offset by (u, v).
     * A fade is drawn as thin strips, each with its own opacity.
     */
    private static void blitFaded(GuiGraphicsExtractor context, Identifier id, UiSettings.Picture picture,
                                  int x, int y, int w, int h, int u, int v, int virtualW, int virtualH) {
        boolean vertical = "top".equals(picture.fade) || "bottom".equals(picture.fade);
        int length = vertical ? h : w;
        int strips = "none".equals(picture.fade) ? 1 : Math.min(FADE_STRIPS, length);
        for (int i = 0; i < strips; i++) {
            int from = length * i / strips, to = length * (i + 1) / strips;
            if (to <= from) continue;
            double center = (from + to) / 2.0 / length;
            int color = UiSettings.color("FFFFFF", fadeOpacity(picture.opacity, picture.fade, picture.fadePercent, center));
            if (vertical) context.blit(RenderPipelines.GUI_TEXTURED, id, x, y + from, u, v + from, w, to - from, w, to - from, virtualW, virtualH, color);
            else context.blit(RenderPipelines.GUI_TEXTURED, id, x + from, y, u + from, v, to - from, h, to - from, h, virtualW, virtualH, color);
        }
    }

    /** Opacity at position {@code t} (0 = top/left, 1 = bottom/right) for a fade toward {@code fade}'s edge. */
    static int fadeOpacity(int opacity, String fade, int fadePercent, double t) {
        double toEdge = switch (fade == null ? "none" : fade) {
            case "top", "left" -> t;
            case "bottom", "right" -> 1 - t;
            default -> 1;
        };
        double reach = Math.clamp(fadePercent, 5, 100) / 100.0;
        return (int) Math.round(opacity * Math.clamp(toEdge / reach, 0, 1));
    }

    public void clear() {
        for (var texture : textures.values()) Minecraft.getInstance().getTextureManager().release(texture.id);
        for (var texture : previousTextures.values()) Minecraft.getInstance().getTextureManager().release(texture.id);
        textures.clear();
        previousTextures.clear();
        rotations.clear();
        failed.clear();
    }
}
