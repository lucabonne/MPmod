package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.state.BossTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class HudLayoutScreen extends Screen {
    private static final float MIN_SCALE = 0.6F;
    private static final float MAX_SCALE = 1.8F;
    private static final float SCALE_STEP = 0.05F;

    private final MinepieceQolClient mod;
    private int draggingPanelId = -1;
    private int draggingOffsetX;
    private int draggingOffsetY;

    public HudLayoutScreen(MinepieceQolClient mod) {
        super(Text.literal(mod == null ? "Minepiece HUD Layout" : mod.tr("hud.layout.title")));
        this.mod = mod;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void removed() {
        this.mod.onHudLayoutEditorClosed();
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_PERIOD) {
            close();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            this.mod.setHudEditSelectedPanel(keyCode - GLFW.GLFW_KEY_0);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
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
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
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
    public boolean mouseReleased(Click click) {
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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float deltaTicks) {
        super.render(drawContext, mouseX, mouseY, deltaTicks);

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
            drawContext.drawTextWithShadow(this.textRenderer, label, rect.x(), Math.max(2, rect.y() - 10), color);
        }
    }

    @Override
    public void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new MinepieceMenuScreen(this.mod));
        }
    }

    private void drawEditorHelp(DrawContext drawContext) {
        String line1 = this.mod.tr("hud.layout.line1");
        String line2 = this.mod.tr("hud.layout.panels") + ": 1 " + this.mod.getHudPanelName(1)
            + " 2 " + this.mod.getHudPanelName(2)
            + " 3 " + this.mod.getHudPanelName(3)
            + " 4 " + this.mod.getHudPanelName(4)
            + " 5 " + this.mod.getHudPanelName(5)
            + " 6 " + this.mod.getHudPanelName(6)
            + " 7 " + this.mod.getHudPanelName(7)
            + " 8 " + this.mod.getHudPanelName(8)
            + " 9 " + this.mod.getHudPanelName(9);
        String line3 = this.mod.tr("hud.layout.line3");
        String line4 = this.mod.tr("hud.layout.line4");
        int width = Math.max(
            Math.max(this.textRenderer.getWidth(line1), this.textRenderer.getWidth(line2)),
            Math.max(this.textRenderer.getWidth(line3), this.textRenderer.getWidth(line4))
        ) + 10;
        int x = 8;
        int y = 8;
        drawContext.fill(x, y, x + width, y + 44, 0xAA000000);
        drawOutline(drawContext, x, y, width, 44, 0x66000000);
        drawContext.drawTextWithShadow(this.textRenderer, line1, x + 5, y + 4, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(this.textRenderer, line2, x + 5, y + 14, 0xFFFFDD88);
        drawContext.drawTextWithShadow(this.textRenderer, line3, x + 5, y + 24, 0xFFAEE8FF);
        drawContext.drawTextWithShadow(this.textRenderer, line4, x + 5, y + 34, 0xFFFFDD88);
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

    private void applyPanelLayout(int panelId, int x, int y, float scale) {
        PanelRect rect = getPanelRectWith(panelId, x, y, scale);
        if (rect == null) {
            return;
        }
        int clampedX = Math.max(-300, Math.min(this.width - 8, rect.x()));
        int clampedY = Math.max(2, Math.min(this.height - 8, rect.y()));
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
        float s = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        int lineStep = Math.max(8, Math.round(10 * s));
        int headerHeight = 12;
        int paddingX = 6;
        int paddingY = 4;

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
                List<String> value = this.mod.getProfileStatsTracker().getHudLines(this.mod::tr);
                yield value.isEmpty() ? List.of(this.mod.tr("common.no_data")) : value;
            }
            case 4 -> {
                List<BossTracker.HudLine> boss = this.mod.getBossTracker().getZoneBossHudLines(false, this.mod::tr);
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
                if (!cfg.inventoryXpHudEnabled) {
                    yield List.of(this.mod.tr("common.hidden"));
                }
                List<String> invXp = this.mod.getInventoryXpTracker().getHudLines(this.mod::tr);
                yield invXp.isEmpty() ? List.of(this.mod.tr("common.no_data")) : invXp;
            }
            default -> List.of();
        };
        if (lines.isEmpty()) {
            lines = List.of(this.mod.tr("common.no_data"));
        }

        String title = this.mod.getHudPanelName(panelId);
        int textWidth = this.textRenderer.getWidth(title);
        for (String line : lines) {
            int width = (int) Math.ceil(this.textRenderer.getWidth(line) * s);
            if (width > textWidth) {
                textWidth = width;
            }
        }
        int panelWidth = textWidth + (paddingX * 2);
        int panelHeight = headerHeight + (paddingY * 2) + (lineStep * lines.size());
        return new PanelRect(x, y, panelWidth, panelHeight);
    }

    private static void drawOutline(DrawContext drawContext, int x, int y, int width, int height, int color) {
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
