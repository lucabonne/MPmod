package net.minepiece.qol.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minepiece.qol.MinepieceQolClient;

public final class HelpScreen extends Screen {
    private final MinepieceQolClient mod;
    private final Screen parent;
    private int scroll;
    private int maxScroll;
    public HelpScreen(MinepieceQolClient mod, Screen parent) { super(Component.literal(mod.tr("extras.help"))); this.mod = mod; this.parent = parent; }
    @Override protected void init() {
        addRenderableWidget(new ThemedButton(mod, Button.builder(Component.literal(mod.tr("ui.done")), b -> onClose()).bounds(width/2-60, height-32, 120,20).build()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int left = Math.max(8, width/2-220), right = Math.min(width-8, left+440);
        g.fill(left-6, 20, right+6, height-42, net.minepiece.qol.config.UiSettings.color(mod.getConfig().appearance.background, mod.getConfig().appearance.menuOpacity));
        int textColor = net.minepiece.qol.config.UiSettings.color(mod.getConfig().appearance.text, 100);
        g.text(font, mod.tr("extras.help"), left, 30, textColor);
        var lines = new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
        for (String key : new String[]{"menus", "layout", "pets", "sort"}) {
            lines.addAll(font.split(Component.literal(mod.tr("extras.help_"+key)), right-left));
            lines.add(net.minecraft.util.FormattedCharSequence.EMPTY);
        }
        maxScroll = Math.max(0, lines.size() * 12 - (height - 96));
        scroll = Math.clamp(scroll, 0, maxScroll);
        g.enableScissor(left, 50, right, height-46);
        for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), left, 50 + i * 12 - scroll, textColor);
        g.disableScissor();
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Math.clamp(scroll - (int) Math.round(vertical * 22), 0, maxScroll);
        return true;
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
