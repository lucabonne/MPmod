package net.minepiece.qol.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.config.UiSettings;
import net.minepiece.qol.state.BossTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class HudOverlay {
    private int PANEL_PADDING_X = 6;
    private int PANEL_PADDING_Y = 4;
    private static final int PANEL_LINE_SPACING = 10;
    private int PANEL_HEADER_HEIGHT = 12;
    private static final int PANEL_BORDER_COLOR = 0x66000000;
    private static final int EDIT_SELECTED_COLOR = 0xCCFFD766;
    private static final int EDIT_UNSELECTED_COLOR = 0x66FFFFFF;
    private static final int GOLD_BORDER_OUTER = 0xCCDEB65A;
    private static final int GOLD_BORDER_INNER = 0xAA9C7A27;
    private static final int GOLD_CORNER_BASE = 0xCCDEB65A;
    private static final int GOLD_CORNER_HIGHLIGHT = 0xFFFFE9B4;
    private static final int GOLD_SPARKLE = 0xFFFFF2CC;
    private static final Map<String, Integer> COLOR_RGB = createColorMap();

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
    private static final PanelTheme MINIBOSSES_THEME = new PanelTheme(
        0xB00B2418, // dark green body
        0xCC4FA875, // outer border
        0xAA1E5A3E, // inner border
        0xCC113322, // header
        0x88528E6E, // header separator
        0xFFFFFFFF,
        0xFFD6FFE6,
        0xCC64BE8A,
        0xFFE3FFF0,
        0xFFF3FFF8
    );
    private static final PanelTheme SCROLLS_THEME = STATS_THEME;

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
        if (!config.modEnabled || !config.allFeaturesVisible) {
            return;
        }

        PANEL_PADDING_X = config.appearance.compact ? 4 : 6;
        PANEL_PADDING_Y = config.appearance.compact ? 2 : 4;
        PANEL_HEADER_HEIGHT = config.appearance.headers ? 12 : 0;
        TextRenderer textRenderer = client.textRenderer;
        for (var picture : config.appearance.pictures) this.mod.getCustomPictures().draw(drawContext, picture, this.mod.isHudEditMode());
        boolean editMode = this.mod.isHudEditMode();
        int selected = this.mod.getHudEditSelectedPanel();
        PanelTheme jobsTheme = resolveThemedPanel(JOBS_THEME, config.jobsHudColor);
        PanelTheme moneyTheme = resolveThemedPanel(MONEY_THEME, config.moneyHudColor);
        PanelTheme statsTheme = resolveThemedPanel(STATS_THEME, config.statsHudColor);
        PanelTheme bossesTheme = resolveThemedPanel(BOSSES_THEME, config.bossHudColor);
        PanelTheme minibossTheme = resolveThemedPanel(MINIBOSSES_THEME, config.minibossHudColor);
        PanelTheme scrollsTheme = resolveThemedPanel(SCROLLS_THEME, config.scrollsHudColor);
        PanelTheme inventoryXpTheme = resolveThemedPanel(MONEY_THEME, config.inventoryXpHudColor);

        List<String> jobsOverviewLines = (config.jobsTrackingEnabled && config.jobsOverviewVisible)
            ? this.mod.getJobsTracker().getOverviewHudLines(this.mod::tr) : List.of();
        List<String> moneyLines = config.moneyTrackingEnabled ? this.mod.getMoneyTracker().getHudLines(this.mod::tr) : List.of();
        List<String> statsLines = config.profileStatsEnabled ? this.mod.getProfileStatsTracker().getHudLines(this.mod::tr) : List.of();
        List<BossTracker.HudLine> bossLines = isBossTrackingEnabled(config) ? this.mod.getBossTracker().getZoneBossHudLines(false, this.mod::tr) : List.of();
        List<BossTracker.HudLine> minibossLines = isBossTrackingEnabled(config) ? this.mod.getBossTracker().getMinibossHudLines(false, this.mod::tr) : List.of();
        List<BossTracker.HudLine> scrollLines = config.scrollsEnabled ? this.mod.getScrollTracker().getHudLines(this.mod::tr) : List.of();
        List<String> inventoryXpLines = this.mod.getXpHudLines();

        PanelRect jobsRect = renderStringPanel(
            drawContext,
            textRenderer,
            this.mod.getHudPanelName(1),
            jobsOverviewLines,
            config.jobsHudX,
            config.jobsHudY,
            config.jobsHudScale,
            jobsTheme,
            editMode,
            selected == 1,
            config.jobsOverviewVisible && this.mod.isHudPanelVisible(1)
        );
        PanelRect moneyRect = renderStringPanel(
            drawContext,
            textRenderer,
            this.mod.getHudPanelName(2),
            moneyLines,
            config.moneyHudX,
            config.moneyHudY,
            config.moneyHudScale,
            moneyTheme,
            editMode,
            selected == 2,
            this.mod.isHudPanelVisible(2)
        );
        PanelRect statsRect = renderStringPanel(
            drawContext,
            textRenderer,
            this.mod.getHudPanelName(3),
            statsLines,
            config.statsHudX,
            config.statsHudY,
            config.statsHudScale,
            statsTheme,
            editMode,
            selected == 3,
            this.mod.isHudPanelVisible(3)
        );
        PanelRect bossesRect = renderToggleableColoredPanel(
            drawContext, textRenderer, this.mod.getHudPanelName(4), bossLines,
            config.bossHudX, config.bossHudY, config.bossHudScale,
            bossesTheme, editMode, selected == 4, this.mod.isHudPanelVisible(4)
        );
        PanelRect minibossRect = renderToggleableColoredPanel(
            drawContext,
            textRenderer,
            this.mod.getHudPanelName(5),
            minibossLines,
            config.minibossHudX,
            config.minibossHudY,
            config.minibossHudScale,
            minibossTheme,
            editMode,
            selected == 5,
            isMinibossHudEnabled(config) && this.mod.isHudPanelVisible(5)
        );
        PanelRect scrollsRect = renderToggleableColoredPanel(
            drawContext,
            textRenderer,
            this.mod.getHudPanelName(8),
            scrollLines,
            config.scrollsHudX,
            config.scrollsHudY,
            config.scrollsHudScale,
            scrollsTheme,
            editMode,
            selected == 8,
            config.scrollsEnabled && this.mod.isHudPanelVisible(8)
        );
        PanelRect inventoryXpRect = renderStringPanel(
            drawContext,
            textRenderer,
            this.mod.getHudPanelName(9),
            inventoryXpLines,
            config.inventoryXpHudX,
            config.inventoryXpHudY,
            config.inventoryXpHudScale,
            inventoryXpTheme,
            editMode,
            selected == 9,
            (config.inventoryXpHudEnabled || config.profileXpHudEnabled) && this.mod.isHudPanelVisible(9)
        );

        PanelRect grindingRect = renderStringPanel(
            drawContext, textRenderer, this.mod.getHudPanelName(10),
            this.mod.getProgressHudController().grinding().getHudLines(this.mod::tr, System.currentTimeMillis()),
            config.grindingHudX, config.grindingHudY, config.grindingHudScale,
            resolveThemedPanel(MONEY_THEME, config.grindingHudColor), editMode, selected == 10, config.grindingHudEnabled && this.mod.isHudPanelVisible(10)
        );

        PanelRect cookingRect = renderStringPanel(
            drawContext, textRenderer, this.mod.getHudPanelName(11),
            this.mod.getCookingTracker().getHudLines(this.mod::tr, config.cookingQuantity),
            config.cookingHudX, config.cookingHudY, config.cookingHudScale,
            resolveThemedPanel(MONEY_THEME, config.cookingHudColor), editMode, selected == 11, config.cookingHudEnabled && this.mod.isHudPanelVisible(11)
        );
        if (editMode && cookingRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "11", cookingRect.x - 7, cookingRect.y - 7, 0xFFFFFFFF);
        }

        if (config.eventsEnabled && (this.mod.isHudPanelVisible(6) || editMode)) {
            int eventColor = resolveConfiguredTextColor(config.eventsHudColor, 0xFFFFFFFF);
            drawScaledText(
                drawContext,
                textRenderer,
                this.mod.getEventCountdownTracker().getDisplayLine(this.mod::tr),
                config.eventsHudX,
                config.eventsHudY,
                eventColor,
                clampScale(config.eventsHudScale)
            );
        }

        String statsHint = this.mod.getStatsRefreshController().getHudHintText();
        if (!statsHint.isBlank() && statsRect != null) {
            int hintY = statsRect.y + statsRect.height + 3;
            drawContext.drawTextWithShadow(textRenderer, statsHint, statsRect.x, hintY, 0xFFAAAAAA);
        }

        String hakiLine = config.hakiEnabled && (this.mod.isHudPanelVisible(7) || editMode) ? this.mod.getCooldownTracker().getHakiHudText(this.mod::tr) : "";
        if (!hakiLine.isBlank()) {
            int hakiDefaultColor = this.mod.getCooldownTracker().isHakiReady() ? 0xFFFF5555 : 0xFFFFFFFF;
            int hakiColor = resolveConfiguredTextColor(config.hakiHudColor, hakiDefaultColor);
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
            drawHudEditorHelp(drawContext, textRenderer, selected, jobsRect, moneyRect, statsRect, bossesRect, minibossRect, scrollsRect, inventoryXpRect, grindingRect);
        }
    }

    private PanelRect renderStringPanel(
        DrawContext drawContext, TextRenderer textRenderer, String title,
        List<String> lines, int x, int y, float scale, PanelTheme theme,
        boolean editMode, boolean selected, boolean enabled
    ) {
        if (!enabled && !editMode) {
            return null;
        }
        List<String> resolved = !enabled ? List.of(this.mod.tr("common.hidden")) : lines;
        List<BossTracker.HudLine> colored = new java.util.ArrayList<>(resolved.size());
        for (String line : resolved) {
            colored.add(new BossTracker.HudLine(line, theme.textColor()));
        }
        return renderColoredPanel(drawContext, textRenderer, title, colored, x, y, scale, theme, editMode, selected);
    }

    private PanelRect renderColoredPanel(
        DrawContext drawContext, TextRenderer textRenderer, String title,
        List<BossTracker.HudLine> lines, int x, int y, float scale,
        PanelTheme theme, boolean editMode, boolean selected
    ) {
        float clampedScale = clampScale(scale);
        List<BossTracker.HudLine> renderLines = lines;
        if (renderLines.isEmpty()) {
            if (!editMode) {
                return null;
            }
            renderLines = List.of(new BossTracker.HudLine(this.mod.tr("common.no_data"), theme.textColor()));
        }

        var appearance = this.mod.getConfig().appearance;
        renderLines = renderLines.stream().map(line -> new BossTracker.HudLine(
            line.text(), appearance.hudTextColor(line.color()))).toList();
        boolean iconRows = this.mod.getConfig().appearance.iconRows;
        var component = iconRows ? new IconGridTooltipComponent(HudIcons.rows(this.mod, title, renderLines),
            this.mod.getConfig().appearance.compact, this.mod.getConfig().appearance.shadows) : null;
        int lineStep = Math.max(8, Math.round((iconRows ? IconGridTooltipComponent.rowHeight(this.mod.getConfig().appearance.compact) : PANEL_LINE_SPACING) * clampedScale));
        int textWidth = 0;
        for (BossTracker.HudLine line : renderLines) {
            int width = (int) Math.ceil(textRenderer.getWidth(line.text()) * clampedScale);
            if (width > textWidth) {
                textWidth = width;
            }
        }
        if (component != null) textWidth = (int) Math.ceil(component.getWidth(textRenderer) * clampedScale);
        int titleWidth = textRenderer.getWidth(title);
        int panelWidth = Math.max(textWidth, titleWidth) + (PANEL_PADDING_X * 2);
        int panelHeight = PANEL_HEADER_HEIGHT + (PANEL_PADDING_Y * 2) + (component == null
            ? renderLines.size() * lineStep : (int) Math.ceil(component.getHeight(textRenderer) * clampedScale));

        drawPanelFrame(drawContext, textRenderer, title, x, y, panelWidth, panelHeight, theme);

        int textX = x + PANEL_PADDING_X;
        int textY = y + PANEL_HEADER_HEIGHT + PANEL_PADDING_Y;
        int offsetY = 0;
        boolean customTheme = isCustomTheme(theme);
        if (component != null) {
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate((float) textX, (float) textY);
            drawContext.getMatrices().scale(clampedScale, clampedScale);
            component.drawText(drawContext, textRenderer, 0, 0);
            component.drawItems(textRenderer, 0, 0, component.getWidth(textRenderer), component.getHeight(textRenderer), drawContext);
            drawContext.getMatrices().popMatrix();
        }
        for (BossTracker.HudLine line : component == null ? renderLines : List.<BossTracker.HudLine>of()) {
            int lineColor = customTheme
                ? ensureReadableColor(line.color(), theme.backgroundColor(), theme.textColor())
                : line.color();
            drawScaledText(drawContext, textRenderer, line.text(), textX, textY + offsetY, lineColor, clampedScale);
            offsetY += lineStep;
        }

        if (editMode) {
            drawOutline(drawContext, x - 1, y - 1, panelWidth + 2, panelHeight + 2, selected ? EDIT_SELECTED_COLOR : EDIT_UNSELECTED_COLOR);
        }
        return new PanelRect(x, y, panelWidth, panelHeight);
    }

    private PanelRect renderToggleableColoredPanel(
        DrawContext drawContext,
        TextRenderer textRenderer,
        String title,
        List<BossTracker.HudLine> lines,
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
        List<BossTracker.HudLine> renderLines = enabled
            ? lines
            : List.of(new BossTracker.HudLine(this.mod.tr("common.hidden"), theme.textColor()));
        return renderColoredPanel(drawContext, textRenderer, title, renderLines, x, y, scale, theme, editMode, selected);
    }

    private void drawPanelFrame(
        DrawContext drawContext,
        TextRenderer textRenderer,
        String title,
        int x,
        int y,
        int width,
        int height,
        PanelTheme theme
    ) {
        var appearance = this.mod.getConfig().appearance;
        drawContext.fill(x, y, x + width, y + height, theme.backgroundColor());
        if (appearance.headers) {
            drawContext.fill(x, y, x + width, y + PANEL_HEADER_HEIGHT, theme.headerColor());
            drawContext.drawText(textRenderer, title, x + 4, y + 2, theme.titleColor(), appearance.shadows);
        }
        for (int i = 0; i < appearance.borderWidth; i++) {
            drawOutline(drawContext, x + i, y + i, width - i * 2, height - i * 2, theme.borderOuterColor());
        }
        if (appearance.decorations) {
            drawCornerOrnaments(drawContext, x, y, width, height, theme);
            drawSparkles(drawContext, x, y, width, height, theme.sparkleColor());
        }
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

    private void drawScaledText(
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
        drawContext.drawText(textRenderer, text, 0, 0, color, this.mod.getConfig().appearance.shadows);
        drawContext.getMatrices().popMatrix();
    }

    private void drawHudEditorHelp(
        DrawContext drawContext,
        TextRenderer textRenderer,
        int selected,
        PanelRect jobsRect,
        PanelRect moneyRect,
        PanelRect statsRect,
        PanelRect bossesRect,
        PanelRect minibossRect,
        PanelRect scrollsRect,
        PanelRect inventoryXpRect,
        PanelRect grindingRect
    ) {
        String selectedName = switch (selected) {
            case 1 -> this.mod.getHudPanelName(1);
            case 2 -> this.mod.getHudPanelName(2);
            case 3 -> this.mod.getHudPanelName(3);
            case 4 -> this.mod.getHudPanelName(4);
            case 5 -> this.mod.getHudPanelName(5);
            case 6 -> this.mod.getHudPanelName(6);
            case 7 -> this.mod.getHudPanelName(7);
            case 8 -> this.mod.getHudPanelName(8);
            case 9 -> this.mod.getHudPanelName(9);
            case 10 -> this.mod.getHudPanelName(10);
            case 11 -> this.mod.getHudPanelName(11);
            default -> this.mod.getHudPanelName(1);
        };
        String controls = this.mod.tr("hud.editor.controls");
        int boxX = 8;
        int boxY = 8;
        int boxW = textRenderer.getWidth(controls) + 8;
        int boxH = 24;
        drawContext.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA000000);
        drawOutline(drawContext, boxX, boxY, boxW, boxH, PANEL_BORDER_COLOR);
        drawContext.drawTextWithShadow(textRenderer, controls, boxX + 4, boxY + 4, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(textRenderer, this.mod.tr("common.selected") + ": " + selectedName, boxX + 4, boxY + 14, 0xFFFFDD88);

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
        if (minibossRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "5", minibossRect.x - 7, minibossRect.y - 7, 0xFFFFFFFF);
        }
        if (scrollsRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "8", scrollsRect.x - 7, scrollsRect.y - 7, 0xFFFFFFFF);
        }
        if (inventoryXpRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "9", inventoryXpRect.x - 7, inventoryXpRect.y - 7, 0xFFFFFFFF);
        }
        if (grindingRect != null) {
            drawContext.drawTextWithShadow(textRenderer, "10", grindingRect.x - 7, grindingRect.y - 7, 0xFFFFFFFF);
        }
    }

    private PanelTheme resolveThemedPanel(PanelTheme fallbackTheme, String configuredColor) {
        var a = this.mod.getConfig().appearance;
        int background = panelBackground(configuredColor, a);
        int opacity = Math.round(a.hudOpacity * 2.55F);
        return new PanelTheme(withAlpha(background, opacity), UiSettings.color(a.border, a.hudOpacity),
            UiSettings.color(a.border, a.hudOpacity), withAlpha(background, opacity),
            UiSettings.color(a.border, a.hudOpacity), UiSettings.color(a.text, 100), a.hudTextColor(UiSettings.color(a.accent, 100)),
            UiSettings.color(a.accent, 100), UiSettings.color(a.text, 100), UiSettings.color(a.accent, 100));
    }

    public static int panelBackground(String configuredColor, UiSettings appearance) {
        String color = MinepieceQolClient.normalizeHudColorName(configuredColor);
        int background = color.startsWith("#") ? Integer.parseInt(color.substring(1), 16)
            : COLOR_RGB.containsKey(color) ? legacyThemedPanel(JOBS_THEME, color).backgroundColor()
            : Integer.parseInt(appearance.background, 16);
        return withAlpha(background, Math.round(appearance.hudOpacity * 2.55F));
    }

    private static PanelTheme legacyThemedPanel(PanelTheme fallbackTheme, String configuredColor) {
        String colorName = MinepieceQolClient.normalizeHudColorName(configuredColor);
        if ("default".equals(colorName)) {
            return fallbackTheme;
        }
        Integer rgb = COLOR_RGB.get(colorName);
        if (rgb == null) {
            return fallbackTheme;
        }

        int bodyRgb = darkenRgb(rgb, 0.78F);
        int headerRgb = darkenRgb(rgb, 0.62F);
        int separatorRgb = mixRgb(bodyRgb, 0xFFFFFF, 0.24F);
        int text = readableTextColor(bodyRgb);
        int title = readableTextColor(headerRgb);

        return new PanelTheme(
            withAlpha(bodyRgb, 0xB0),
            GOLD_BORDER_OUTER,
            GOLD_BORDER_INNER,
            withAlpha(headerRgb, 0xCC),
            withAlpha(separatorRgb, 0x88),
            text,
            title,
            GOLD_CORNER_BASE,
            GOLD_CORNER_HIGHLIGHT,
            GOLD_SPARKLE
        );
    }

    private static boolean isCustomTheme(PanelTheme theme) {
        return theme.borderOuterColor() == GOLD_BORDER_OUTER && theme.cornerBaseColor() == GOLD_CORNER_BASE;
    }

    private static int ensureReadableColor(int preferredColor, int backgroundColor, int fallbackColor) {
        int bgRgb = backgroundColor & 0xFFFFFF;
        int fgRgb = preferredColor & 0xFFFFFF;
        int diff = Math.abs(luma(fgRgb) - luma(bgRgb));
        return diff < 70 ? fallbackColor : preferredColor;
    }

    private static int luma(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    private static int readableTextColor(int rgb) {
        return luma(rgb) > 140 ? 0xFF1B1B1B : 0xFFFFFFFF;
    }

    private static int resolveConfiguredTextColor(String configuredColor, int fallback) {
        String normalized = MinepieceQolClient.normalizeHudColorName(configuredColor);
        if ("default".equals(normalized)) {
            return fallback;
        }
        Integer rgb = COLOR_RGB.get(normalized);
        return rgb == null ? fallback : withAlpha(rgb, 0xFF);
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static int darkenRgb(int rgb, float factor) {
        int r = Math.max(0, Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor)));
        int g = Math.max(0, Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor)));
        int b = Math.max(0, Math.min(255, Math.round((rgb & 0xFF) * factor)));
        return (r << 16) | (g << 8) | b;
    }

    private static int mixRgb(int rgbA, int rgbB, float weightB) {
        float weightA = 1.0F - weightB;
        int r = Math.max(0, Math.min(255, Math.round(((rgbA >> 16) & 0xFF) * weightA + ((rgbB >> 16) & 0xFF) * weightB)));
        int g = Math.max(0, Math.min(255, Math.round(((rgbA >> 8) & 0xFF) * weightA + ((rgbB >> 8) & 0xFF) * weightB)));
        int b = Math.max(0, Math.min(255, Math.round((rgbA & 0xFF) * weightA + (rgbB & 0xFF) * weightB)));
        return (r << 16) | (g << 8) | b;
    }

    private static Map<String, Integer> createColorMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("blue", 0x4B77FF);
        map.put("red", 0xE34A4A);
        map.put("purple", 0x8A5CF6);
        map.put("yellow", 0xEBC53A);
        map.put("pink", 0xF56BB6);
        map.put("green", 0x4CAF50);
        map.put("orange", 0xF28C28);
        map.put("lime", 0x8BCF3F);
        map.put("aqua", 0x33C7C9);
        map.put("navy", 0x1E3A8A);
        map.put("coral", 0xFF7F50);
        map.put("teal", 0x1F9E89);
        map.put("mustard", 0xC9A227);
        map.put("blue violet", 0x6F42C1);
        map.put("black", 0x1A1A1A);
        map.put("white", 0xEFEFEF);
        map.put("grey", 0x808080);
        map.put("brown", 0x7B4A2F);
        map.put("dark green", 0x1F5E2D);
        map.put("blue gray", 0x607D8B);
        map.put("indigo", 0x3F51B5);
        map.put("pea green", 0x6FA833);
        map.put("amber", 0xFFBF00);
        map.put("peach", 0xFFB07C);
        map.put("maroon", 0x7A1F3D);
        return map;
    }

    private static boolean isBossTrackingEnabled(ConfigManager.ModConfig config) {
        return config.bossTrackingEnabled == null || config.bossTrackingEnabled;
    }

    private static boolean isMinibossHudEnabled(ConfigManager.ModConfig config) {
        return config.minibossHudEnabled == null || config.minibossHudEnabled;
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
