package net.minepiece.qol.ui;

import java.util.List;
import java.util.function.ToIntFunction;
import net.minepiece.qol.config.UiSettings;

/** Shared dimensions for drawing, editor selection and island-heading clicks. */
public record HudPanelGeometry(int x, int y, int width, int height, int textX, int textY,
                               int rowHeight, int lineStep, float scale, boolean iconRows, int headerHeight, float titleScale) {
    public static HudPanelGeometry measure(String title, List<String> lines, int x, int y, float scale,
                                          UiSettings style, ToIntFunction<String> textWidth) {
        return measure(-1, title, lines, x, y, scale, style, textWidth);
    }

    public static HudPanelGeometry measure(int panelId, String title, List<String> lines, int x, int y, float scale,
                                          UiSettings style, ToIntFunction<String> textWidth) {
        float overall = Math.clamp(scale, 0.6F, 1.8F);
        var panel = style.panelStyle(panelId);
        float s = overall * panel.textScale;
        if (panelId == 6 || panelId == 7) {
            int width = lines.stream().mapToInt(textWidth).max().orElse(1);
            return new HudPanelGeometry(x, y, (int) Math.ceil(width * s), (int) Math.ceil(8 * s),
                x, y, 8, (int) Math.ceil(8 * s), s, false, 0, 1);
        }
        int px = style.compact ? 4 : 6;
        int py = style.compact ? 2 : 4;
        int header = panel.titleVisible(style.headers) ? (int) Math.ceil(12 * panel.textScale) : 0;
        int row = style.iconRows ? IconGridTooltipComponent.rowHeight(style.compact) : 10;
        int step = Math.max(8, (int) Math.ceil(row * s));
        int column = style.iconRows ? IconGridTooltipComponent.iconColumn(style.compact) : 0;
        int width = header > 0 ? (int) Math.ceil(textWidth.applyAsInt(title) * panel.textScale) : 0;
        for (String line : lines) width = Math.max(width, (int) Math.ceil((textWidth.applyAsInt(line) + column) * s));
        int height = header + py * 2 + (style.iconRows ? (int) Math.ceil(lines.size() * row * s) : lines.size() * step);
        return new HudPanelGeometry(x, y, Math.max(width + px * 2, Math.round(panel.minWidth * overall)),
            Math.max(height, Math.round(panel.minHeight * overall)), x + px, y + header + py, row, step, s, style.iconRows, header, panel.textScale);
    }
    public RowRect row(int index) {
        int top = this.textY + (this.iconRows ? (int) Math.floor(index * this.rowHeight * this.scale) : index * this.lineStep);
        int bottom = this.textY + (this.iconRows ? (int) Math.floor((index + 1) * this.rowHeight * this.scale) : (index + 1) * this.lineStep);
        return new RowRect(this.x, top, this.width, bottom - top);
    }
    public record RowRect(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
