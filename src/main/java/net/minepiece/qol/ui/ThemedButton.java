package net.minepiece.qol.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.UiSettings;

/** Keeps vanilla button actions and narration, without an opaque vanilla background. */
public final class ThemedButton extends AbstractWidget {
    private final MinepieceQolClient mod;
    private final Button button;

    public ThemedButton(MinepieceQolClient mod, Button button) {
        super(button.getX(), button.getY(), button.getWidth(), button.getHeight(), button.getMessage());
        this.mod = mod; this.button = button;
        this.active = button.active; this.visible = button.visible;
    }

    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.active = button.active; this.visible = button.visible;
        var a = this.mod.getConfig().appearance;
        int x = getX(), y = getY(), width = getWidth(), height = getHeight();
        boolean hover = isMouseOver(mouseX, mouseY) || isFocused();
        context.fill(x, y, x + width, y + height, UiSettings.color(a.background, a.menuOpacity));
        context.fill(x, y, x + width, y + height, UiSettings.color(a.accent, a.menuOpacity * (hover && active ? 35 : 15) / 100));
        int color = UiSettings.color(a.border, a.menuOpacity);
        for (int i = 0; i < a.borderWidth; i++) {
            context.fill(x + i, y + i, x + width - i, y + i + 1, color);
            context.fill(x + i, y + height - i - 1, x + width - i, y + height - i, color);
            context.fill(x + i, y + i, x + i + 1, y + height - i, color);
            context.fill(x + width - i - 1, y + i, x + width - i, y + height - i, color);
        }
        var renderer = Minecraft.getInstance().font;
        String caption = renderer.plainSubstrByWidth(button.getMessage().getString(), Math.max(1, width - 6));
        context.text(renderer, caption, x + (width - renderer.width(caption)) / 2,
            y + (height - 8) / 2, UiSettings.color(a.text, active ? 100 : 50), a.shadows);
    }

    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { }

    @Override public void onClick(MouseButtonEvent click, boolean doubled) { button.onClick(click, doubled); }
    @Override public boolean keyPressed(KeyEvent input) {
        button.setFocused(isFocused());
        return button.keyPressed(input);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput builder) { button.updateWidgetNarration(builder); }
}
