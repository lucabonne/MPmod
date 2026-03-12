package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.state.BossTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
        super(Text.literal("Minepiece HUD Layout"));
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_PERIOD) {
            close();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_6) {
            this.mod.setHudEditSelectedPanel(keyCode - GLFW.GLFW_KEY_0);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int panelId = findPanelAt((int) mouseX, (int) mouseY);
        if (panelId <= 0) {
            return false;
        }

        PanelRect rect = getPanelRect(panelId);
        if (rect == null) {
            return false;
        }
        this.mod.setHudEditSelectedPanel(panelId);
        this.draggingPanelId = panelId;
        this.draggingOffsetX = (int) mouseX - rect.x();
        this.draggingOffsetY = (int) mouseY - rect.y();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || this.draggingPanelId <= 0) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        int newX = (int) mouseX - this.draggingOffsetX;
        int newY = (int) mouseY - this.draggingOffsetY;
        float scale = this.mod.getHudPanelScale(this.draggingPanelId);
        applyPanelLayout(this.draggingPanelId, newX, newY, scale);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.draggingPanelId > 0) {
            this.draggingPanelId = -1;
            this.mod.saveConfig();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
            client.setScreen(null);
        }
    }

    private void drawEditorHelp(DrawContext drawContext) {
        String line1 = "HUD Editor: drag panels with mouse, wheel to resize.";
        String line2 = "Panels: 1 Jobs 2 Money 3 Stats 4 Bosses 5 Event 6 Haki | . or ESC to close";
        int width = Math.max(this.textRenderer.getWidth(line1), this.textRenderer.getWidth(line2)) + 10;
        int x = 8;
        int y = 8;
        drawContext.fill(x, y, x + width, y + 24, 0xAA000000);
        drawOutline(drawContext, x, y, width, 24, 0x66000000);
        drawContext.drawTextWithShadow(this.textRenderer, line1, x + 5, y + 4, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(this.textRenderer, line2, x + 5, y + 14, 0xFFFFDD88);
    }

    private int findPanelAt(int mouseX, int mouseY) {
        List<Integer> ids = new ArrayList<>();
        for (int panelId = 1; panelId <= this.mod.getHudPanelCount(); panelId++) {
            ids.add(panelId);
        }
        // Prefer top-most visually: last IDs generally render after.
        for (int i = ids.size() - 1; i >= 0; i--) {
            int panelId = ids.get(i);
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
                    yield List.of("Hidden");
                }
                List<String> value = this.mod.getJobsTracker().getOverviewHudLines();
                yield value.isEmpty() ? List.of("No data") : value;
            }
            case 2 -> {
                List<String> value = this.mod.getMoneyTracker().getHudLines();
                yield value.isEmpty() ? List.of("No data") : value;
            }
            case 3 -> {
                List<String> value = this.mod.getProfileStatsTracker().getHudLines();
                yield value.isEmpty() ? List.of("No data") : value;
            }
            case 4 -> {
                List<BossTracker.HudLine> boss = this.mod.getBossTracker().getHudLines(false);
                if (boss.isEmpty()) {
                    yield List.of("No data");
                }
                List<String> rendered = new ArrayList<>(boss.size());
                for (BossTracker.HudLine line : boss) {
                    rendered.add(line.text());
                }
                yield rendered;
            }
            case 5 -> List.of(this.mod.getEventCountdownTracker().getDisplayLine());
            case 6 -> {
                String hakiLine = this.mod.getCooldownTracker().getHakiHudText();
                yield List.of(hakiLine.isBlank() ? "Haki ready" : hakiLine);
            }
            default -> List.of();
        };
        if (lines.isEmpty()) {
            lines = List.of("No data");
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
