package net.minepiece.qol.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minepiece.qol.state.RarityDetector;

/** Shared grid for item tooltips and HUD panels, with a real sprite column and aligned text. */
public final class IconGridTooltipComponent implements ClientTooltipComponent {
    public record Row(String text, int color, ItemStack item, Identifier sprite) { }
    private final List<Row> rows;
    private final int rowHeight;
    private final int iconSize;
    private final boolean shadow;
    /** When above 0, "Label: value" rows put the value flush right inside this width. */
    private int alignWidth;
    /** Chooses the value's color from (label, value, row color); null keeps the row color. */
    private ValueColor valueColor;

    public interface ValueColor {
        int color(String label, String value, int fallback);
    }

    public IconGridTooltipComponent colorValues(ValueColor valueColor) {
        this.valueColor = valueColor;
        return this;
    }

    public IconGridTooltipComponent(List<Row> rows, boolean compact, boolean shadow) {
        this.rows = List.copyOf(rows);
        this.rowHeight = compact ? 12 : 18;
        this.iconSize = compact ? 10 : 16;
        this.shadow = shadow;
    }
    public IconGridTooltipComponent alignValues(int width) {
        this.alignWidth = width;
        return this;
    }

    /** Splits "Label: value" at its last ": ", or returns null when the row has no value. */
    public static String[] splitValue(String text) {
        int at = text == null ? -1 : text.lastIndexOf(": ");
        if (at <= 0 || at + 2 >= text.length()) return null;
        return new String[]{text.substring(0, at), text.substring(at + 2)};
    }

    public static int rowHeight(boolean compact) { return compact ? 12 : 18; }
    public static int iconColumn(boolean compact) { return compact ? 14 : 20; }
    private int column() { return this.iconSize + 4; }
    @Override public int getHeight(Font renderer) { return this.rows.size() * this.rowHeight; }
    @Override public int getWidth(Font renderer) {
        return this.rows.stream().mapToInt(row -> renderer.width(row.text())).max().orElse(0) + column();
    }
    @Override public void extractText(GuiGraphicsExtractor context, Font renderer, int x, int y) {
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            int textY = y + i * this.rowHeight + (this.rowHeight - 9) / 2;
            String[] parts = this.alignWidth > 0 ? splitValue(row.text()) : null;
            if (parts == null) {
                context.text(renderer, row.text(), x + column(), textY, row.color(), this.shadow);
                continue;
            }
            context.text(renderer, parts[0], x + column(), textY, row.color(), this.shadow);
            int valueColor = this.valueColor == null ? row.color() : this.valueColor.color(parts[0], parts[1], row.color());
            context.text(renderer, parts[1], x + this.alignWidth - renderer.width(parts[1]), textY, valueColor, this.shadow);
        }
    }
    @Override public void extractImage(Font renderer, int x, int y, int width, int height, GuiGraphicsExtractor context) {
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            int iy = y + i * this.rowHeight;
            if (row.sprite() != null) {
                context.blit(RenderPipelines.GUI_TEXTURED, row.sprite(), x, iy, 0, 0, this.iconSize, this.iconSize, this.iconSize, this.iconSize);
            } else if (row.item() != null && !row.item().isEmpty()) {
                context.pose().pushMatrix();
                context.pose().translate((float) x, (float) iy);
                context.pose().scale(this.iconSize / 16F, this.iconSize / 16F);
                context.item(row.item(), 0, 0);
                context.pose().popMatrix();
                var mod = net.minepiece.qol.MinepieceQolClient.get();
                if (mod != null && mod.getConfig().rarityIconsEnabled) {
                    RarityDetector.detect(row.item()).ifPresent(rarity -> RarityBadges.draw(context, rarity,
                        x + this.iconSize - 5, iy + this.iconSize - 5, 5));
                }
            }
        }
    }
}
