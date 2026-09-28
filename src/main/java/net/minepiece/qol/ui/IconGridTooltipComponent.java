package net.minepiece.qol.ui;

import java.util.List;
import net.minepiece.qol.state.RarityDetector;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Shared grid for item tooltips and HUD panels, with a real sprite column and aligned text. */
public final class IconGridTooltipComponent implements TooltipComponent {
    public record Row(String text, int color, ItemStack item, Identifier sprite) { }
    private final List<Row> rows;
    private final int rowHeight;
    private final int iconSize;
    private final boolean shadow;

    public IconGridTooltipComponent(List<Row> rows, boolean compact, boolean shadow) {
        this.rows = List.copyOf(rows);
        this.rowHeight = compact ? 12 : 18;
        this.iconSize = compact ? 10 : 16;
        this.shadow = shadow;
    }
    public static int rowHeight(boolean compact) { return compact ? 12 : 18; }
    public static int iconColumn(boolean compact) { return compact ? 14 : 20; }
    private int column() { return this.iconSize + 4; }
    @Override public int getHeight(TextRenderer renderer) { return this.rows.size() * this.rowHeight; }
    @Override public int getWidth(TextRenderer renderer) {
        return this.rows.stream().mapToInt(row -> renderer.getWidth(row.text())).max().orElse(0) + column();
    }
    @Override public void drawText(DrawContext context, TextRenderer renderer, int x, int y) {
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            context.drawText(renderer, row.text(), x + column(), y + i * this.rowHeight + (this.rowHeight - 9) / 2,
                row.color(), this.shadow);
        }
    }
    @Override public void drawItems(TextRenderer renderer, int x, int y, int width, int height, DrawContext context) {
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            int iy = y + i * this.rowHeight;
            if (row.sprite() != null) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, row.sprite(), x, iy, 0, 0, this.iconSize, this.iconSize, this.iconSize, this.iconSize);
            } else if (row.item() != null && !row.item().isEmpty()) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate((float) x, (float) iy);
                context.getMatrices().scale(this.iconSize / 16F, this.iconSize / 16F);
                context.drawItem(row.item(), 0, 0);
                context.getMatrices().popMatrix();
                var mod = net.minepiece.qol.MinepieceQolClient.get();
                if (mod != null && mod.getConfig().rarityIconsEnabled) {
                    RarityDetector.detect(row.item()).ifPresent(rarity -> RarityBadges.draw(context, rarity,
                        x + this.iconSize - 5, iy + this.iconSize - 5, 5));
                }
            }
        }
    }
}
