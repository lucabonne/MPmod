package net.minepiece.qol.ui;

import java.util.List;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.state.BossTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class HudOverlay {
    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_Y = 4;
    private static final int PANEL_LINE_SPACING = 10;
    private static final int PANEL_HEADER_HEIGHT = 12;
    private static final int PANEL_BORDER_COLOR = 0x66000000;
    private static final int EDIT_SELECTED_COLOR = 0xCCFFD766;
    private static final int EDIT_UNSELECTED_COLOR = 0x66FFFFFF;

    private static final PanelTheme JOBS_THEME = new PanelTheme(
        0xB01E1534, // purple body
        0xCC7A5BC0, // outer border
        0xAA3B2C60, // inner border
        0xCC2F2152, // header
        0x884D3D78, // header separator
        0xFFFFFFFF,
        0xFFE8DCFF,
        0xCC9B7CE0, // corner base
        0xFFEBD9FF, // corner highlight
        0xFFF8F4FF  // sparkle
    );
    private static final PanelTheme MONEY_THEME = new PanelTheme(
        0xB02A1C08, // orange body
        0xCCD49A49, // outer border
        0xAA5E3F17, // inner border
        0xCC3E2B10, // header
        0x887A5527, // header separator
        0xFFFFFFFF,
        0xFFFFE0B0,
        0xCCEDB95F,
        0xFFFFF1C7,
        0xFFFFF8E2
    );
    private static final PanelTheme STATS_THEME = new PanelTheme(
        0xB02B0A12, // red body
        0xCCBD6777, // outer border
        0xAA5D2230, // inner border
        0xCC41131F, // header
        0x88853D4B, // header separator
        0xFFFFFFFF,
        0xFFFFD6DF,
        0xCCD9879A,
        0xFFFFE5EA,
        0xFFFFF5F8
    );
    private static final PanelTheme BOSSES_THEME = new PanelTheme(
        0xB00B1329, // blue body
        0xCC6C95D6, // outer border
        0xAA253B66, // inner border
        0xCC111F3F, // header
        0x8842679C, // header separator
        0xFFFFFFFF,
        0xFFD8E6FF,
        0xCC86AFE9,
        0xFFE6F0FF,
        0xFFF6FAFF
    );

    private final MinepieceQolClient mod;

    public HudOverlay(MinepieceQolClient mod) {
        this.mod = mod;
    }

    public void render(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }

        ConfigManager.ModConfig config = this.mod.getConfig();
        if (!config.allFeaturesVisible) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        boolean editMode = this.mod.isHudEditMode();
        int selected = this.mod.getHudEditSelectedPanel();

        List<String> jobsOverviewLines = config.jobsOverviewVisible ? this.mod.getJobsTracker().getOverviewHudLines() : List.of();
        List<String> moneyLines = this.mod.getMoneyTracker().getHudLines();
        List<String> statsLines = this.mod.getProfileStatsTracker().getHudLines();
        List<BossTracker.HudLine> bossLines = this.mod.getBossTracker().getHudLines(false);

        PanelRect jobsRect = renderStringPanel(
            drawContext,
            textRenderer,
            "Jobs",
            jobsOverviewLines,
            config.jobsHudX,
            config.jobsHudY,
            config.jobsHudScale,
            JOBS_THEME,
            editMode,
            selected == 1,
            config.jobsOverviewVisible
        );
        PanelRect moneyRect = renderStringPanel(
            drawContext,
            textRenderer,
            "Money",
            moneyLines,
            config.moneyHudX,
            config.moneyHudY,
            config.moneyHudScale,
            MONEY_THEME,
            editMode,
            selected == 2,
            true
        );
        PanelRect statsRect = renderStringPanel(
            drawContext,
            textRenderer,
            "Stats",
            statsLines,
            config.statsHudX,
            config.statsHudY,
            config.statsHudScale,
            STATS_THEME,
            editMode,
            selected == 3,
            true
        );
        PanelRect bossesRect = renderBossPanel(
            drawContext,
            textRenderer,
            "Bosses",
            bossLines,
            config.bossHudX,
            config.bossHudY,
            config.bossHudScale,
            BOSSES_THEME,
            editMode,
            selected == 4
        );

        drawScaledText(
            drawContext,
            textRenderer,
            this.mod.getEventCountdownTracker().getDisplayLine(),
            config.eventsHudX,
            config.eventsHudY,
            0xFFFFFFFF,
            clampScale(config.eventsHudScale)
        );

        String statsHint = this.mod.getStatsRefreshController().getHudHintText();
        if (!statsHint.isBlank() && statsRect != null) {
            int hintY = statsRect.y + statsRect.height + 3;
            drawContext.drawTextWithShadow(textRenderer, statsHint, statsRect.x, hintY, 0xFFAAAAAA);
        }

        String hakiLine = this.mod.getCooldownTracker().getHakiHudText();
        if (!hakiLine.isBlank()) {
            int hakiColor = this.mod.getCooldownTracker().isHakiReady() ? 0xFFFF5555 : 0xFFFFFFFF;
            int hakiX = this.mod.resolveHakiHudX(client);
            int hakiY = this.mod.resolveHakiHudY(client);
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate((float) hakiX, (float) hakiY);
            float hakiScale = clampScale(config.hakiHudScale);
            drawContext.getMatrices().scale(hakiScale, hakiScale);
            drawContext.drawTextWithShadow(textRenderer, hakiLine, 0, 0, hakiColor);
            drawContext.getMatrices().popMatrix();
        }

        if (editMode) {
            drawHudEditorHelp(drawContext, textRenderer, selected, jobsRect, moneyRect, statsRect, bossesRect);
        }
    }

    private static PanelRect renderStringPanel(
        DrawContext drawContext,
        TextRenderer textRenderer,
        String title,
        List<String> lines,
        int x,
        int y,
        float scale,
        PanelTheme theme,
        boolean editMode,
        boolean selected,
        boolean enabled
    ) {
        if (!enabled && !editMode) {
            return null;
        }

        float clampedScale = clampScale(scale);
        List<String> renderLines = lines;
        if (!enabled) {
            renderLines = List.of("Hidden");
        } else if (renderLines.isEmpty()) {
            if (!editMode) {
                return null;
            }
            renderLines = List.of("No data");
        }

        int lineStep = Math.max(8, Math.round(PANEL_LINE_SPACING * clampedScale));
        int textWidth = 0;
        for (String line : renderLines) {
            int width = (int) Math.ceil(textRenderer.getWidth(line) * clampedScale);
            if (width > textWidth) {
                textWidth = width;
            }
        }
        int titleWidth = textRenderer.getWidth(title);
        int panelWidth = Math.max(textWidth, titleWidth) + (PANEL_PADDING_X * 2);
        int panelHeight = PANEL_HEADER_HEIGHT + (PANEL_PADDING_Y * 2) + (renderLines.size() * lineStep);

        drawPanelFrame(drawContext, textRenderer, title, x, y, panelWidth, panelHeight, theme);

        int textX = x + PANEL_PADDING_X;
        int textY = y + PANEL_HEADER_HEIGHT + PANEL_PADDING_Y;
        int offsetY = 0;
        for (String line : renderLines) {
            drawScaledText(drawContext, textRenderer, line, textX, textY + offsetY, theme.textColor(), clampedScale);
            offsetY += lineStep;
        }

        if (editMode) {
            drawOutline(drawContext, x - 1, y - 1, panelWidth + 2, panelHeight + 2, selected ? EDIT_SELECTED_COLOR : EDIT_UNSELECTED_COLOR);
        }
        return new PanelRect(x, y, panelWidth, panelHeight);
    }

    private static PanelRect renderBossPanel(
        DrawContext drawContext,
        TextRenderer textRenderer,
        String title,
        List<BossTracker.HudLine> lines,
        int x,
        int y,
        float scale,
        PanelTheme theme,
        boolean editMode,
        boolean selected
    ) {
        float clampedScale = clampScale(scale);
        List<BossTracker.HudLine> renderLines = lines;
        if (renderLines.isEmpty()) {
            if (!editMode) {
                return null;
            }
            renderLines = List.of(new BossTracker.HudLine("No data", theme.textColor()));
        }

        int lineStep = Math.max(8, Math.round(PANEL_LINE_SPACING * clampedScale));
        int textWidth = 0;
        for (BossTracker.HudLine line : renderLines) {
            int width = (int) Math.ceil(textRenderer.getWidth(line.text()) * clampedScale);
            if (width > textWidth) {
                textWidth = width;
            }
        }
        int titleWidth = textRenderer.getWidth(title);
        int panelWidth = Math.max(textWidth, titleWidth) + (PANEL_PADDING_X * 2);
        int panelHeight = PANEL_HEADER_HEIGHT + (PANEL_PADDING_Y * 2) + (renderLines.size() * lineStep);

        drawPanelFrame(drawContext, textRenderer, title, x, y, panelWidth, panelHeight, theme);

        int textX = x + PANEL_PADDING_X;
        int textY = y + PANEL_HEADER_HEIGHT + PANEL_PADDING_Y;
        int offsetY = 0;
        for (BossTracker.HudLine line : renderLines) {
            drawScaledText(drawContext, textRenderer, line.text(), textX, textY + offsetY, line.color(), clampedScale);
            offsetY += lineStep;
        }

        if (editMode) {
            drawOutline(drawContext, x - 1, y - 1, panelWidth + 2, panelHeight + 2, selected ? EDIT_SELECTED_COLOR : EDIT_UNSELECTED_COLOR);
        }
        return new PanelRect(x, y, panelWidth, panelHeight);
    }

    private static void drawPanelFrame(
        DrawContext drawContext,
        TextRenderer textRenderer,
        String title,
        int x,
        int y,
        int width,
        int height,
        PanelTheme theme
    ) {
        drawContext.fill(x, y, x + width, y + height, theme.backgroundColor());
        drawContext.fill(x, y, x + width, y + PANEL_HEADER_HEIGHT, theme.headerColor());
        drawOutline(drawContext, x, y, width, height, theme.borderOuterColor());
        if (width > 4 && height > 4) {
            drawOutline(drawContext, x + 1, y + 1, width - 2, height - 2, theme.borderInnerColor());
        }
        drawContext.fill(x + 2, y + PANEL_HEADER_HEIGHT, x + width - 2, y + PANEL_HEADER_HEIGHT + 1, theme.headerSeparatorColor());
        drawCornerOrnaments(drawContext, x, y, width, height, theme);
        drawSparkles(drawContext, x, y, width, height, theme.sparkleColor());
        drawContext.drawTextWithShadow(textRenderer, title, x + 4, y + 2, theme.titleColor());
    }

    private static void drawOutline(DrawContext drawContext, int x, int y, int width, int height, int color) {
        drawContext.fill(x, y, x + width, y + 1, color);
        drawContext.fill(x, y + height - 1, x + width, y + height, color);
        drawContext.fill(x, y, x + 1, y + height, color);
        drawContext.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void drawCornerOrnaments(DrawContext drawContext, int x, int y, int width, int height, PanelTheme theme) {
        int s = 4;
        // top-left
        drawContext.fill(x, y, x + s, y + 1, theme.cornerBaseColor());
        drawContext.fill(x, y, x + 1, y + s, theme.cornerBaseColor());
        drawContext.fill(x + 1, y + 1, x + s - 1, y + 2, theme.cornerHighlightColor());
        drawContext.fill(x + 1, y + 1, x + 2, y + s - 1, theme.cornerHighlightColor());
        // top-right
        drawContext.fill(x + width - s, y, x + width, y + 1, theme.cornerBaseColor());
        drawContext.fill(x + width - 1, y, x + width, y + s, theme.cornerBaseColor());
        drawContext.fill(x + width - s + 1, y + 1, x + width - 1, y + 2, theme.cornerHighlightColor());
        drawContext.fill(x + width - 2, y + 1, x + width - 1, y + s - 1, theme.cornerHighlightColor());
        // bottom-left
        drawContext.fill(x, y + height - 1, x + s, y + height, theme.cornerBaseColor());
        drawContext.fill(x, y + height - s, x + 1, y + height, theme.cornerBaseColor());
        drawContext.fill(x + 1, y + height - 2, x + s - 1, y + height - 1, theme.cornerHighlightColor());
        drawContext.fill(x + 1, y + height - s + 1, x + 2, y + height - 1, theme.cornerHighlightColor());
        // bottom-right
        drawContext.fill(x + width - s, y + height - 1, x + width, y + height, theme.cornerBaseColor());
        drawContext.fill(x + width - 1, y + height - s, x + width, y + height, theme.cornerBaseColor());
        drawContext.fill(x + width - s + 1, y + height - 2, x + width - 1, y + height - 1, theme.cornerHighlightColor());
        drawContext.fill(x + width - 2, y + height - s + 1, x + width - 1, y + height - 1, theme.cornerHighlightColor());
    }

    private static void drawSparkles(DrawContext drawContext, int x, int y, int width, int height, int sparkleColor) {
        drawPlus(drawContext, x + Math.max(6, width - 12), y + 3, sparkleColor);
        drawPlus(drawContext, x + Math.max(4, width - 8), y + Math.max(8, height - 6), sparkleColor);
    }

    private static void drawPlus(DrawContext drawContext, int x, int y, int color) {
        drawContext.fill(x - 1, y, x + 2, y + 1, color);
        drawContext.fill(x, y - 1, x + 1, y + 2, color);
    }

    private static void drawScaledText(
        DrawContext drawContext,
        TextRenderer textRenderer,
        String text,
        int x,
        int y,
        int color,
        float scale
    ) {
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate((float) x, (float) y);
        drawContext.getMatrices().scale(scale, scale);
        drawContext.drawTextWithShadow(textRenderer, text, 0, 0, color);
        drawContext.getMatrices().popMatrix();
    }

    private static void drawHudEditorHelp(
        DrawContext drawContext,
        TextRenderer textRenderer,
        int selected,
        PanelRect jobsRect,
        PanelRect moneyRect,
        PanelRect statsRect,
        PanelRect bossesRect
    ) {
        String selectedName = switch (selected) {
            case 1 -> "Jobs";
            case 2 -> "Money";
            case 3 -> "Stats";
            case 4 -> "Bosses";
            case 5 -> "Event Timer";
            case 6 -> "Haki";
            default -> "Jobs";
        };
        String controls = "HUD Edit (.): drag with mouse, wheel resize, 1-6 select, . closes";
        int boxX = 8;
        int boxY = 8;
        int boxW = textRenderer.getWidth(controls) + 8;
        int boxH = 24;
        drawContext.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA000000);
        drawOutline(drawContext, boxX, boxY, boxW, boxH, PANEL_BORDER_COLOR);
        drawContext.drawTextWithShadow(textRenderer, controls, boxX + 4, boxY + 4, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(textRenderer, "Selected: " + selectedName, boxX + 4, boxY + 14, 0xFFFFDD88);

        if (jobsRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "1", jobsRect.x - 7, jobsRect.y - 7, 0xFFFFFFFF);
        }
        if (moneyRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "2", moneyRect.x - 7, moneyRect.y - 7, 0xFFFFFFFF);
        }
        if (statsRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "3", statsRect.x - 7, statsRect.y - 7, 0xFFFFFFFF);
        }
        if (bossesRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "4", bossesRect.x - 7, bossesRect.y - 7, 0xFFFFFFFF);
        }
    }

    private static float clampScale(float scale) {
        return Math.max(0.6F, Math.min(1.8F, scale));
    }

    private record PanelTheme(
        int backgroundColor,
        int borderOuterColor,
        int borderInnerColor,
        int headerColor,
        int headerSeparatorColor,
        int textColor,
        int titleColor,
        int cornerBaseColor,
        int cornerHighlightColor,
        int sparkleColor
    ) {
    }

    private record PanelRect(int x, int y, int width, int height) {
    }
}
