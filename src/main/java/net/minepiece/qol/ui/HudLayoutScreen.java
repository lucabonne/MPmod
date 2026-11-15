package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.state.BossTracker;
import org.lwjgl.glfw.GLFW;

public final class HudLayoutScreen extends Screen {
    private static final float MIN_SCALE = 0.6F;
    private static final float MAX_SCALE = 1.8F;
    private static final float SCALE_STEP = 0.05F;

    private final MinepieceQolClient mod;
    private final Screen parent;
    private int draggingPanelId = -1;
    private int draggingOffsetX;
    private int draggingOffsetY;

    public HudLayoutScreen(MinepieceQolClient mod) {
        super(Component.literal(mod == null ? "Minepiece HUD Layout" : mod.tr("hud.layout.title")));
        this.mod = mod;
        this.parent = Minecraft.getInstance().gui.screen();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void removed() {
        this.mod.onHudLayoutEditorClosed();
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        int keyCode = keyInput.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_PERIOD) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_H) {
            int id = this.mod.getHudEditSelectedPanel();
            var picture = this.mod.getHudPicture(id);
            if (picture != null) picture.visible = !picture.visible;
            else if (!this.mod.getConfig().appearance.hiddenPanels.remove(Integer.valueOf(id)))
                this.mod.getConfig().appearance.hiddenPanels.add(id);
            this.mod.saveConfig();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            this.mod.setHudEditSelectedPanel(keyCode - GLFW.GLFW_KEY_0);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && this.mod.toggleBossIslandAt(click.x(), click.y())) return true;
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubled);
        }

        int panelId = findPanelAt((int) click.x(), (int) click.y());
        if (panelId <= 0) {
            return false;
        }

        PanelRect rect = getPanelRect(panelId);
        if (rect == null) {
            return false;
        }
        this.mod.setHudEditSelectedPanel(panelId);
        this.draggingPanelId = panelId;
        this.draggingOffsetX = (int) click.x() - rect.x();
        this.draggingOffsetY = (int) click.y() - rect.y();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || this.draggingPanelId <= 0) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        int newX = (int) click.x() - this.draggingOffsetX;
        int newY = (int) click.y() - this.draggingOffsetY;
        float scale = this.mod.getHudPanelScale(this.draggingPanelId);
        applyPanelLayout(this.draggingPanelId, newX, newY, scale);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.draggingPanelId > 0) {
            this.draggingPanelId = -1;
            this.mod.saveConfig();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int hovered = findPanelAt((int) mouseX, (int) mouseY);
        int panelId = hovered > 0 ? hovered : this.mod.getHudEditSelectedPanel();
        if (panelId <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        this.mod.setHudEditSelectedPanel(panelId);
        var picture = this.mod.getHudPicture(panelId);
        if (picture != null) {
            if (!picture.fullscreen) zoomPicture(panelId, picture, verticalAmount > 0.0D);
            return true;
        }
        float currentScale = this.mod.getHudPanelScale(panelId);
        float nextScale = (float) (currentScale + (verticalAmount > 0.0D ? SCALE_STEP : -SCALE_STEP));
        nextScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, nextScale));
        int x = this.mod.getHudPanelX(panelId);
        int y = this.mod.getHudPanelY(panelId);
        applyPanelLayout(panelId, x, y, nextScale);
        this.mod.saveConfig();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(drawContext, mouseX, mouseY, deltaTicks);

        drawContext.fill(0, 0, this.width, this.height, 0x28000000);
        drawEditorHelp(drawContext);

        for (int panelId = 1; panelId <= this.mod.getHudPanelCount(); panelId++) {
            PanelRect rect = getPanelRect(panelId);
            if (rect == null) {
                continue;
            }
            boolean selected = panelId == this.mod.getHudEditSelectedPanel();
            int color = selected ? 0xCCFFD766 : 0x88FFFFFF;
            drawOutline(drawContext, rect.x() - 1, rect.y() - 1, rect.width() + 2, rect.height() + 2, color);
            String label = panelId + ". " + this.mod.getHudPanelName(panelId)
                + " x=" + rect.x() + " y=" + rect.y()
                + " s=" + String.format(java.util.Locale.ROOT, "%.2f", this.mod.getHudPanelScale(panelId));
            drawContext.text(this.font, label, rect.x(), Math.max(2, rect.y() - 10), color);
        }
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.gui.setScreen(this.parent);
        }
    }

    private void drawEditorHelp(GuiGraphicsExtractor drawContext) {
        String line1 = this.mod.tr("hud.layout.line1");
        String line2 = this.mod.tr("common.selected") + ": " + this.mod.getHudPanelName(this.mod.getHudEditSelectedPanel());
        String line3 = String.format(java.util.Locale.ROOT, this.mod.tr("hud.layout.shortcut"), this.mod.getHudEditorShortcutName());
        String line4 = this.mod.tr("ui.image_edit_hint");
        int width = Math.max(
            Math.max(this.font.width(line1), this.font.width(line2)),
            Math.max(this.font.width(line3), this.font.width(line4))
        ) + 10;
        int x = 8;
        int y = 8;
        drawContext.fill(x, y, x + width, y + 44, 0xAA000000);
        drawOutline(drawContext, x, y, width, 44, 0x66000000);
        drawContext.text(this.font, line1, x + 5, y + 4, 0xFFFFFFFF);
        drawContext.text(this.font, line2, x + 5, y + 14, 0xFFFFDD88);
        drawContext.text(this.font, line3, x + 5, y + 24, 0xFFAEE8FF);
        drawContext.text(this.font, line4, x + 5, y + 34, 0xFFFFDD88);
    }

    private int findPanelAt(int mouseX, int mouseY) {
        for (int panelId = this.mod.getHudPanelCount(); panelId >= 1; panelId--) {
            PanelRect rect = getPanelRect(panelId);
            if (rect != null && rect.contains(mouseX, mouseY)) {
                return panelId;
            }
        }
        return -1;
    }

    /** Pictures zoom by 10% per notch around their center, up to the size that covers the whole screen. */
    private void zoomPicture(int panelId, net.minepiece.qol.config.UiSettings.Picture picture, boolean in) {
        float cover = net.minepiece.qol.config.UiSettings.coverScale(picture, this.width, this.height);
        float scale = Math.clamp(picture.scale * (in ? 1.1F : 1 / 1.1F),
            net.minepiece.qol.config.UiSettings.PICTURE_MIN_SCALE, cover);
        float centerX = picture.x + picture.width * picture.scale / 2, centerY = picture.y + picture.height * picture.scale / 2;
        int x = Math.round(centerX - picture.width * scale / 2), y = Math.round(centerY - picture.height * scale / 2);
        if (scale >= cover) { // covering the screen: center it so the crop is even
            x = Math.round((this.width - picture.width * scale) / 2);
            y = Math.round((this.height - picture.height * scale) / 2);
        }
        applyPanelLayout(panelId, x, y, scale);
        this.mod.saveConfig();
    }

    private void applyPanelLayout(int panelId, int x, int y, float scale) {
        PanelRect rect = getPanelRectWith(panelId, x, y, scale);
        if (rect == null) {
            return;
        }
        // A zoomed picture may be bigger than the screen, so it can hang off the top/left edge.
        boolean picture = this.mod.getHudPicture(panelId) != null;
        int clampedX = Math.max(picture ? Math.min(0, this.width - rect.width()) : -300, Math.min(this.width - 8, rect.x()));
        int clampedY = Math.max(picture ? Math.min(2, this.height - rect.height()) : 2, Math.min(this.height - 8, rect.y()));
        this.mod.setHudPanelLayout(panelId, clampedX, clampedY, scale);
    }

    private PanelRect getPanelRect(int panelId) {
        return getPanelRectWith(
            panelId,
            this.mod.getHudPanelX(panelId),
            this.mod.getHudPanelY(panelId),
            this.mod.getHudPanelScale(panelId)
        );
    }

    private PanelRect getPanelRectWith(int panelId, int x, int y, float scale) {
        var picture = this.mod.getHudPicture(panelId);
        if (picture != null && picture.fullscreen) return null; // covers the screen: not draggable
        if (picture != null) return new PanelRect(x, y, Math.round(picture.width * scale), Math.round(picture.height * scale));
        float s = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        List<String> lines = switch (panelId) {
            case 1 -> {
                ConfigManager.ModConfig cfg = this.mod.getConfig();
                if (!cfg.jobsOverviewVisible) {
                    yield List.of(this.mod.tr("common.hidden"));
                }
                List<String> value = this.mod.getJobsTracker().getOverviewHudLines(this.mod::tr);
                yield value.isEmpty() ? List.of(this.mod.tr("common.no_data")) : value;
            }
            case 2 -> {
                List<String> value = this.mod.getMoneyTracker().getHudLines(this.mod::tr);
                yield value.isEmpty() ? List.of(this.mod.tr("common.no_data")) : value;
            }
            case 3 -> {
                List<String> value = this.mod.getProfileStatsTracker().getHudLines(this.mod::tr, this.mod.getConfig().appearance.profileStats);
                yield value.isEmpty() ? List.of(this.mod.tr("common.no_data")) : value;
            }
            case 4 -> {
                List<BossTracker.HudLine> boss = this.mod.getBossTracker().getZoneBossHudLines(false, this.mod::tr, this.mod.getConfig().appearance.collapsedBossIslands, this.mod.getConfig().appearance.bossesByTime);
                if (boss.isEmpty()) {
                    yield List.of(this.mod.tr("common.no_data"));
                }
                List<String> rendered = new ArrayList<>(boss.size());
                for (BossTracker.HudLine line : boss) {
                    rendered.add(line.text());
                }
                yield rendered;
            }
            case 5 -> {
                if (!this.mod.isMinibossHudEnabled()) {
                    yield List.of(this.mod.tr("common.hidden"));
                }
                List<BossTracker.HudLine> miniboss = this.mod.getBossTracker().getMinibossHudLines(false, this.mod::tr);
                if (miniboss.isEmpty()) {
                    yield List.of(this.mod.tr("common.no_data"));
                }
                List<String> rendered = new ArrayList<>(miniboss.size());
                for (BossTracker.HudLine line : miniboss) {
                    rendered.add(line.text());
                }
                yield rendered;
            }
            case 6 -> List.of(this.mod.getEventCountdownTracker().getDisplayLine(this.mod::tr));
            case 7 -> {
                String hakiLine = this.mod.getCooldownTracker().getHakiHudText(this.mod::tr);
                yield List.of(hakiLine.isBlank() ? this.mod.tr("common.ready") : hakiLine);
            }
            case 8 -> {
                ConfigManager.ModConfig cfg = this.mod.getConfig();
                if (!cfg.scrollsEnabled) {
                    yield List.of(this.mod.tr("common.hidden"));
                }
                List<BossTracker.HudLine> scrolls = this.mod.getScrollTracker().getHudLines(this.mod::tr);
                if (scrolls.isEmpty()) {
                    yield List.of(this.mod.tr("common.no_data"));
                }
                List<String> rendered = new ArrayList<>(scrolls.size());
                for (BossTracker.HudLine line : scrolls) {
                    rendered.add(line.text());
                }
                yield rendered;
            }
            case 9 -> {
                ConfigManager.ModConfig cfg = this.mod.getConfig();
                if (!cfg.inventoryXpHudEnabled && !cfg.profileXpHudEnabled) {
                    yield List.of(this.mod.tr("common.hidden"));
                }
                List<String> invXp = this.mod.getXpHudLines();
                yield invXp.isEmpty() ? List.of(this.mod.tr("common.no_data")) : invXp;
            }
            case 12 -> ClockHud.lines();
            case 13 -> this.mod.getEssentials().quests().lines();
            case 14 -> this.mod.getEssentials().petLines();
            case 15 -> this.mod.getSafeChestHudLines().stream().map(BossTracker.HudLine::text).toList();
            case 11 -> this.mod.getConfig().cookingHudEnabled
                ? this.mod.getCookingTracker().getHudLines(this.mod::tr, this.mod.getConfig().cookingQuantity) : List.of(this.mod.tr("common.hidden"));
            case 10 -> this.mod.getConfig().grindingHudEnabled
                ? this.mod.getProgressHudController().grinding().getHudLines(this.mod::tr, System.currentTimeMillis()) : List.of(this.mod.tr("common.hidden"));
            default -> List.of();
        };
        if (lines.isEmpty()) {
            lines = List.of(this.mod.tr("common.no_data"));
        }

        var cfg = this.mod.getConfig();
        boolean enabled = switch (panelId) {
            case 1 -> cfg.jobsOverviewVisible; case 5 -> this.mod.isMinibossHudEnabled(); case 8 -> cfg.scrollsEnabled;
            case 9 -> cfg.inventoryXpHudEnabled || cfg.profileXpHudEnabled; case 10 -> cfg.grindingHudEnabled;
            case 11 -> cfg.cookingHudEnabled; case 13 -> cfg.dailyQuestsEnabled; case 14 -> cfg.activePetsEnabled;
            case 15 -> cfg.safeChestsEnabled;
            default -> true;
        };
        boolean hasData = switch (panelId) {
            case 1 -> cfg.jobsTrackingEnabled; case 2 -> cfg.moneyTrackingEnabled; case 3 -> cfg.profileStatsEnabled;
            case 4, 5 -> this.mod.isBossTrackingEnabled(); default -> true;
        };
        if (!hasData) lines = List.of(this.mod.tr("common.no_data"));
        if (panelId != 6 && panelId != 7 && (!enabled || !this.mod.isHudPanelVisible(panelId))) lines = List.of(this.mod.tr("common.hidden"));
        var geometry = HudPanelGeometry.measure(panelId, this.mod.getHudPanelName(panelId), lines, x, y, s,
            cfg.appearance, this.font::width);
        return new PanelRect(x, y, geometry.width(), geometry.height());
    }

    private static void drawOutline(GuiGraphicsExtractor drawContext, int x, int y, int width, int height, int color) {
        drawContext.fill(x, y, x + width, y + 1, color);
        drawContext.fill(x, y + height - 1, x + width, y + height, color);
        drawContext.fill(x, y, x + 1, y + height, color);
        drawContext.fill(x + width - 1, y, x + width, y + height, color);
    }

    private record PanelRect(int x, int y, int width, int height) {
        boolean contains(int px, int py) {
            return px >= this.x && py >= this.y && px < this.x + this.width && py < this.y + this.height;
        }
    }
}
