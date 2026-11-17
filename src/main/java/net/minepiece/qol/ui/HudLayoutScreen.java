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

    private static final int GOLD = 0xFFDDBA6A;
    private static final int GRID = 4;
    private static final int UNDO_LIMIT = 50;
    /** Remembered while the game runs, so the editor reopens the way it was left. */
    private static boolean snap = true;
    private static boolean panelListOpen = false;

    private final MinepieceQolClient mod;
    private final Screen parent;
    private final java.util.ArrayDeque<Snapshot> undo = new java.util.ArrayDeque<>();
    private int lastScrollPanel = -1;
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

    /** No blur: the panels being edited are drawn underneath this screen and must stay sharp. */
    @Override
    public void extractBackground(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float deltaTicks) {
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
        if (keyCode == GLFW.GLFW_KEY_Z && keyInput.hasControlDown()) {
            undo();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_H) {
            pushUndo();
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
        for (ToolButton button : toolbar()) {
            if (button.contains(click.x(), click.y())) {
                button.action().run();
                return true;
            }
        }
        if (panelListOpen) {
            int row = panelListRowAt(click.x(), click.y());
            if (row > 0) {
                this.mod.setHudEditSelectedPanel(row);
                return true;
            }
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
        pushUndo();
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
        PanelRect rect = getPanelRect(this.draggingPanelId);
        if (snap && !click.hasShiftDown() && rect != null) {
            newX = Math.round(newX / (float) GRID) * GRID;
            newY = Math.round(newY / (float) GRID) * GRID;
            // pull toward the screen's center lines
            if (Math.abs(newX + rect.width() / 2 - this.width / 2) <= GRID) newX = this.width / 2 - rect.width() / 2;
            if (Math.abs(newY + rect.height() / 2 - this.height / 2) <= GRID) newY = this.height / 2 - rect.height() / 2;
        }
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
        if (this.lastScrollPanel != panelId) pushUndo(); // one undo step per run of scrolling on a panel
        this.lastScrollPanel = panelId;
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
        if (snap) drawGrid(drawContext);

        int selectedId = this.mod.getHudEditSelectedPanel();
        for (int panelId = 1; panelId <= this.mod.getHudPanelCount(); panelId++) {
            PanelRect rect = getPanelRect(panelId);
            if (rect == null || panelId == selectedId) {
                continue;
            }
            drawOutline(drawContext, rect.x() - 1, rect.y() - 1, rect.width() + 2, rect.height() + 2, 0x66FFFFFF);
        }
        PanelRect selected = getPanelRect(selectedId);
        if (selected != null) drawSelection(drawContext, selectedId, selected);
        drawToolbar(drawContext, mouseX, mouseY);
        if (panelListOpen) drawPanelList(drawContext, mouseX, mouseY);
    }

    private void drawGrid(GuiGraphicsExtractor g) {
        for (int x = 0; x < this.width; x += GRID * 2) g.fill(x, 0, x + 1, this.height, 0x0FFFFFFF);
        for (int y = 0; y < this.height; y += GRID * 2) g.fill(0, y, this.width, y + 1, 0x0FFFFFFF);
        g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x55DDBA6A);
        g.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x55DDBA6A);
    }

    /** Gold outline, corner handles and a label with name, position and size. */
    private void drawSelection(GuiGraphicsExtractor g, int panelId, PanelRect rect) {
        drawOutline(g, rect.x() - 1, rect.y() - 1, rect.width() + 2, rect.height() + 2, GOLD);
        int[][] corners = {{rect.x() - 2, rect.y() - 2}, {rect.x() + rect.width() - 1, rect.y() - 2},
            {rect.x() - 2, rect.y() + rect.height() - 1}, {rect.x() + rect.width() - 1, rect.y() + rect.height() - 1}};
        for (int[] c : corners) g.fill(c[0], c[1], c[0] + 3, c[1] + 3, GOLD);
        String name = this.mod.getHudPanelName(panelId);
        String info = "x " + rect.x() + "  y " + rect.y() + "  "
            + String.format(java.util.Locale.ROOT, "%.2f×", this.mod.getHudPanelScale(panelId));
        int nameWidth = this.font.width(name) + 6, infoWidth = this.font.width(info) + 6;
        int labelY = rect.y() + rect.height() + 3;
        if (labelY + 11 > this.height) labelY = rect.y() - 14;
        int labelX = Math.clamp(rect.x(), 2, Math.max(2, this.width - nameWidth - infoWidth - 4));
        g.fill(labelX, labelY, labelX + nameWidth, labelY + 11, GOLD);
        g.text(this.font, name, labelX + 3, labelY + 2, 0xFF1A1206, false);
        g.fill(labelX + nameWidth + 2, labelY, labelX + nameWidth + 2 + infoWidth, labelY + 11, 0xEE0B1838);
        drawOutline(g, labelX + nameWidth + 2, labelY, infoWidth, 11, 0xFF3D78E0);
        g.text(this.font, info, labelX + nameWidth + 5, labelY + 2, 0xFFEAF1FF);
    }

    private record ToolButton(String label, int x, int y, int width, int height, boolean on, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }
    }

    private List<ToolButton> toolbar() {
        String[] labels = {this.mod.tr("hud.editor.snap"), this.mod.tr("hud.editor.arrange"), this.mod.tr("hud.editor.undo"),
            this.mod.tr("hud.editor.reset"), this.mod.tr("hud.editor.panels"), this.mod.tr("hud.editor.done")};
        Runnable[] actions = {
            () -> snap = !snap,
            () -> { pushUndo(); autoArrange(); this.mod.saveConfig(); },
            this::undo,
            () -> { pushUndo(); this.mod.resetHudLayoutToDefaults(); this.mod.saveConfig(); },
            () -> panelListOpen = !panelListOpen,
            this::onClose
        };
        boolean[] on = {snap, false, false, false, panelListOpen, false};
        int gap = 3, height = 13, total = -gap;
        int[] widths = new int[labels.length];
        for (int i = 0; i < labels.length; i++) total += (widths[i] = this.font.width(labels[i]) + 10) + gap;
        int x = (this.width - total) / 2;
        List<ToolButton> buttons = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            buttons.add(new ToolButton(labels[i], x, 4, widths[i], height, on[i], actions[i]));
            x += widths[i] + gap;
        }
        return buttons;
    }

    private void drawToolbar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<ToolButton> buttons = toolbar();
        ToolButton first = buttons.getFirst(), last = buttons.getLast();
        g.fill(first.x() - 3, 1, last.x() + last.width() + 3, 20, 0xEE0B1838);
        drawOutline(g, first.x() - 3, 1, last.x() + last.width() + 6 - first.x(), 19, 0xFF3D78E0);
        for (int i = 0; i < buttons.size(); i++) {
            ToolButton b = buttons.get(i);
            boolean done = i == buttons.size() - 1;
            boolean hover = b.contains(mouseX, mouseY);
            int bg = done ? ThemedSwitch.ON : hover ? 0xFF1F4A8C : 0xFF16396E;
            g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), bg);
            drawOutline(g, b.x(), b.y(), b.width(), b.height(), done ? ThemedSwitch.ON_BORDER : b.on() ? GOLD : 0xFF2A4F96);
            g.text(this.font, b.label(), b.x() + 5, b.y() + 3, 0xFFFFFFFF);
        }
        String hints = String.format(java.util.Locale.ROOT, this.mod.tr("hud.editor.hints"), this.mod.getHudEditorShortcutName());
        hints = this.font.plainSubstrByWidth(hints, this.width - 8);
        int hintsX = (this.width - this.font.width(hints)) / 2;
        g.fill(hintsX - 3, 21, hintsX + this.font.width(hints) + 3, 32, 0xCC050818);
        g.text(this.font, hints, hintsX, 23, 0xFFAFC4EA);
    }

    private static final int LIST_ROW = 10, LIST_WIDTH = 130;

    private int panelListTop() {
        return this.height - 4 - this.mod.getHudPanelCount() * LIST_ROW - 4;
    }

    /** The panel id under the mouse in the panel list, or -1. */
    private int panelListRowAt(double mx, double my) {
        int top = panelListTop() + 2;
        if (mx < 4 || mx >= 4 + LIST_WIDTH || my < top) return -1;
        int row = (int) ((my - top) / LIST_ROW) + 1;
        return row <= this.mod.getHudPanelCount() ? row : -1;
    }

    private void drawPanelList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int count = this.mod.getHudPanelCount(), top = panelListTop();
        g.fill(4, top, 4 + LIST_WIDTH, this.height - 4, 0xEE0B1838);
        drawOutline(g, 4, top, LIST_WIDTH, this.height - 4 - top, 0xFF3D78E0);
        int hovered = panelListRowAt(mouseX, mouseY);
        for (int id = 1; id <= count; id++) {
            int y = top + 2 + (id - 1) * LIST_ROW;
            boolean selected = id == this.mod.getHudEditSelectedPanel();
            if (selected || id == hovered) g.fill(5, y, 3 + LIST_WIDTH, y + LIST_ROW, selected ? 0x44DDBA6A : 0x331F4FB8);
            var picture = this.mod.getHudPicture(id);
            boolean hidden = picture != null ? !picture.visible : !this.mod.isHudPanelVisible(id);
            String state = hidden ? this.mod.tr("common.hidden") : String.format(java.util.Locale.ROOT, "%.2f×", this.mod.getHudPanelScale(id));
            int stateWidth = this.font.width(state);
            String name = this.font.plainSubstrByWidth(this.mod.getHudPanelName(id), LIST_WIDTH - stateWidth - 14);
            g.text(this.font, name, 8, y + 1, selected ? 0xFFFFE7A8 : 0xFFEAF1FF);
            g.text(this.font, state, 4 + LIST_WIDTH - stateWidth - 4, y + 1, 0xFF89A8DA);
        }
    }

    private record Snapshot(float[][] layout, List<Integer> hidden, List<Boolean> pictures) { }

    private void pushUndo() {
        int count = this.mod.getHudPanelCount();
        float[][] layout = new float[count + 1][];
        for (int id = 1; id <= count; id++)
            layout[id] = new float[]{this.mod.getHudPanelX(id), this.mod.getHudPanelY(id), this.mod.getHudPanelScale(id)};
        var appearance = this.mod.getConfig().appearance;
        this.undo.push(new Snapshot(layout, List.copyOf(appearance.hiddenPanels),
            appearance.pictures.stream().map(p -> p.visible).toList()));
        while (this.undo.size() > UNDO_LIMIT) this.undo.removeLast();
    }

    private void undo() {
        Snapshot last = this.undo.poll();
        if (last == null) return;
        this.lastScrollPanel = -1;
        int count = Math.min(this.mod.getHudPanelCount(), last.layout().length - 1);
        for (int id = 1; id <= count; id++) {
            float[] p = last.layout()[id];
            this.mod.setHudPanelLayout(id, Math.round(p[0]), Math.round(p[1]), p[2]);
        }
        var appearance = this.mod.getConfig().appearance;
        appearance.hiddenPanels.clear();
        appearance.hiddenPanels.addAll(last.hidden());
        for (int i = 0; i < Math.min(appearance.pictures.size(), last.pictures().size()); i++)
            appearance.pictures.get(i).visible = last.pictures().get(i);
        this.mod.saveConfig();
    }

    /**
     * Stacks the visible panels in columns, alternating between the left and right screen edges
     * so the middle (crosshair, hotbar) stays clear.
     */
    public void autoArrange() {
        int leftX = 4, rightX = this.width - 4, y = 4, columnWidth = 0;
        boolean right = false;
        for (int id = 1; id <= this.mod.getHudPanelCount(); id++) {
            if (this.mod.getHudPicture(id) != null || !this.mod.isHudPanelVisible(id)) continue;
            PanelRect rect = getPanelRect(id);
            if (rect == null || !rect.live()) continue;
            int x = right ? rightX - rect.width() : leftX;
            int bottom = x < this.width / 2 + 95 && x + rect.width() > this.width / 2 - 95 ? this.height - 50 : this.height - 4;
            if (y > 4 && y + rect.height() > bottom) {
                if (right) rightX -= columnWidth + 4; else leftX += columnWidth + 4;
                right = !right;
                y = 4;
                columnWidth = 0;
                x = right ? rightX - rect.width() : leftX;
            }
            this.mod.setHudPanelLayout(id, x, y, this.mod.getHudPanelScale(id));
            y += rect.height() + 4;
            columnWidth = Math.max(columnWidth, rect.width());
        }
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.gui.setScreen(this.parent);
        }
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
        if (picture != null) return new PanelRect(x, y, Math.round(picture.width * scale), Math.round(picture.height * scale), true);
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
        String noData = this.mod.tr("common.no_data"), hidden = this.mod.tr("common.hidden");
        boolean live = lines.stream().anyMatch(line -> !line.isBlank() && !line.equals(noData) && !line.equals(hidden));
        return new PanelRect(x, y, geometry.width(), geometry.height(), live);
    }

    private static void drawOutline(GuiGraphicsExtractor drawContext, int x, int y, int width, int height, int color) {
        drawContext.fill(x, y, x + width, y + 1, color);
        drawContext.fill(x, y + height - 1, x + width, y + height, color);
        drawContext.fill(x, y, x + 1, y + height, color);
        drawContext.fill(x + width - 1, y, x + width, y + height, color);
    }

    /** {@code live}: the panel has something to show in game (not empty, "no data" or hidden). */
    private record PanelRect(int x, int y, int width, int height, boolean live) {
        boolean contains(int px, int py) {
            return px >= this.x && py >= this.y && px < this.x + this.width && py < this.y + this.height;
        }
    }
}
