package net.minepiece.qol.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.UiSettings;

/** A vanilla checkbox drawn as a setting row: label on the left, on/off switch on the right. */
public final class ThemedSwitch extends AbstractWidget {
    static final int ON = 0xFF2E8B57, ON_BORDER = 0xFF6FD39A, OFF = 0xFF1A2B49, OFF_BORDER = 0xFF3A5378;
    private final MinepieceQolClient mod;
    private final Checkbox checkbox;

    public ThemedSwitch(MinepieceQolClient mod, Checkbox checkbox, int width) {
        super(checkbox.getX(), checkbox.getY(), width, Math.max(14, checkbox.getHeight() - 2), checkbox.getMessage());
        this.mod = mod;
        this.checkbox = checkbox;
        this.active = checkbox.active;
        this.visible = checkbox.visible;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        var a = this.mod.getConfig().appearance;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean on = checkbox.selected();
        boolean hover = isMouseOver(mouseX, mouseY) || isFocused();
        boolean labelled = !getMessage().getString().isEmpty();
        if (labelled) {
            context.fill(x, y, x + w, y + h, UiSettings.color(a.accent, a.menuOpacity * (hover ? 30 : 14) / 100));
            context.fill(x, y + h - 1, x + w, y + h, UiSettings.color(a.border, a.menuOpacity / 2));
        }
        int sw = labelled ? 22 : Math.min(16, w), sh = labelled ? 10 : 9;
        int sx = x + w - sw - (labelled ? 4 : 0), sy = y + (h - sh) / 2;
        context.fill(sx, sy, sx + sw, sy + sh, on ? ON_BORDER : OFF_BORDER);
        context.fill(sx + 1, sy + 1, sx + sw - 1, sy + sh - 1, on ? ON : OFF);
        int knob = sh - 4;
        int kx = on ? sx + sw - 2 - knob : sx + 2;
        context.fill(kx, sy + 2, kx + knob, sy + 2 + knob, on ? 0xFFFFFFFF : 0xFF89A8DA);
        if (labelled) {
            var font = Minecraft.getInstance().font;
            String text = font.plainSubstrByWidth(getMessage().getString(), Math.max(1, sx - x - 10));
            context.text(font, text, x + 5, y + (h - 8) / 2, UiSettings.color(a.text, active ? 100 : 50), a.shadows);
        }
    }

    @Override public void onClick(MouseButtonEvent click, boolean doubled) { checkbox.onClick(click, doubled); }
    @Override public boolean keyPressed(KeyEvent input) {
        checkbox.setFocused(isFocused());
        return checkbox.keyPressed(input);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput builder) { checkbox.updateWidgetNarration(builder); }
}
