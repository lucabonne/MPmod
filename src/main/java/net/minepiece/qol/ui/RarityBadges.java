package net.minepiece.qol.ui;

import java.util.Locale;
import net.minepiece.qol.state.RarityDetector.Rarity;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class RarityBadges {
    private RarityBadges() { }

    public static int color(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xFF79D990;
            case RARE -> 0xFF92BFF0;
            case EPIC -> 0xFFCA92ED;
            case LEGENDARY -> 0xFFFFCC55;
            case MYTHIC -> 0xFFFF788E;
            case PRIMORDIAL -> 0xFFFFE69B;
        };
    }

    public static void draw(DrawContext context, Rarity rarity, int x, int y, int size) {
        if (rarity == Rarity.PRIMORDIAL) {
            // Original geometric badge, kept crisp at inventory icon sizes.
            for (int row = 0; row < size; row++) {
                int inset = Math.abs(row - size / 2);
                int color = row < size / 2 ? 0xFFFFE69B : 0xFFFF964F;
                context.fill(x + inset, y + row, x + size - inset, y + row + 1, color);
            }
            context.fill(x + size / 2 - 1, y + size / 3, x + size / 2 + 1, y + size * 2 / 3, 0xFFFFFFFF);
            return;
        }
        Identifier texture = Identifier.of("minepiece-qol", "textures/gui/rarity/" + rarity.name().toLowerCase(Locale.ROOT) + ".png");
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, size, size);
    }
}
