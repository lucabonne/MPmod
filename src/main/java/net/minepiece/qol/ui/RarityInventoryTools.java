package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.essentials.RaritySort;
import net.minepiece.qol.state.RarityDetector;
import net.minepiece.qol.state.RarityDetector.Rarity;

/** Aure64's filtering/sorting controls, reusing QoL badges. Sorting is restricted to vanilla storage. */
public final class RarityInventoryTools {
    private static final EnumSet<Rarity> FILTER = EnumSet.noneOf(Rarity.class);
    private record Hit(int x, int y, Rarity rarity, int action) { boolean contains(double x, double y) { return x >= this.x && x < this.x + 14 && y >= this.y && y < this.y + 14; } }
    private static final List<Hit> hits = new ArrayList<>();
    private static AbstractContainerScreen<?> current;
    private static ItemStack[] expected, desired;
    private static AbstractContainerScreen<?> sorting;
    private static long nextSwap;
    private static boolean ascending;
    private RarityInventoryTools() { }
    public static void register(MinepieceQolClient mod) {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof AbstractContainerScreen<?> container) {
                ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
                    if (click.button() != 0 || current != container || !enabled(mod)) return true;
                    for (var hit : hits) if (hit.contains(click.x(), click.y())) {
                        if (hit.action == 0) { if (!FILTER.remove(hit.rarity)) FILTER.add(hit.rarity); }
                        else if (hit.action == 1) FILTER.clear();
                        else startSort(container, hit.action == 2 ? RaritySort.Mode.RARITY : RaritySort.Mode.ITEM);
                        return false;
                    }
                    if (sorting == container) sorting = null;
                    return true;
                });
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled(mod)) { FILTER.clear(); hits.clear(); current = null; sorting = null; return; }
            tickSort(client, mod);
            if (client.gui.screen() != current) { current = null; FILTER.clear(); hits.clear(); }
        });
    }
    private static boolean enabled(MinepieceQolClient mod) { return mod != null && mod.getConfig().modEnabled && mod.getConfig().allFeaturesVisible && mod.getEssentials() != null && mod.getEssentials().onServer(); }
    public static void dimSlot(MinepieceQolClient mod, GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        if (!enabled(mod) || !mod.getConfig().rarityFilterEnabled || FILTER.isEmpty()) return;
        if (RarityDetector.detect(stack).filter(FILTER::contains).isEmpty()) g.fill(x, y, x + 16, y + 16, 0xBA080808);
    }
    public static void draw(MinepieceQolClient mod, AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top, int imageWidth, int mouseX, int mouseY) {
        if (!enabled(mod)) return;
        if (current != screen) { current = screen; FILTER.clear(); }
        hits.clear();
        int x = left + imageWidth + 4;
        if (x + 14 > screen.width) x = Math.max(0, left - 18);
        int y = top;
        if (mod.getConfig().rarityFilterEnabled) {
            var present = EnumSet.noneOf(Rarity.class);
            screen.getMenu().slots.forEach(s -> RarityDetector.detect(s.getItem()).ifPresent(present::add));
            for (var rarity : Rarity.values()) {
                if (!present.contains(rarity)) continue;
                g.fill(x - 1, y - 1, x + 15, y + 15, FILTER.contains(rarity) ? 0xFFFFFFFF : 0xFF303030);
                g.fill(x, y, x + 14, y + 14, 0xFF101723);
                RarityBadges.draw(g, rarity, x + 2, y + 2, 10);
                hits.add(new Hit(x, y, rarity, 0)); y += 16;
            }
            if (!FILTER.isEmpty()) { drawButton(g, x, y, "×"); hits.add(new Hit(x,y,null,1)); y += 16; }
        }
        if (mod.getConfig().inventorySortingEnabled && storageSize(screen) > 0) {
            drawButton(g, x, y, ascending ? "↑" : "↓"); hits.add(new Hit(x,y,null,2)); y += 16;
            drawButton(g, x, y, "A"); hits.add(new Hit(x,y,null,3));
        }
        for (var hit : hits) if (hit.contains(mouseX, mouseY)) {
            String label = hit.action == 0 ? mod.tr("extras.filter") + ": " + hit.rarity.name() : mod.tr(hit.action == 1 ? "extras.clear_filter" : hit.action == 2 ? "extras.sort_rarity" : "extras.sort_name");
            g.setTooltipForNextFrame(Minecraft.getInstance().font, Component.literal(label), mouseX, mouseY);
        }
    }
    private static void drawButton(GuiGraphicsExtractor g, int x, int y, String text) {
        g.fill(x, y, x+14, y+14, 0xEE101723); g.text(Minecraft.getInstance().font, text, x+3, y+3, 0xFFFFFFFF);
    }
    public static void drawHotbar(MinepieceQolClient mod, GuiGraphicsExtractor g) {
        var mc = Minecraft.getInstance();
        if (!enabled(mod) || !mod.getConfig().hotbarRarityEnabled || mc.player == null || mc.player.isSpectator()) return;
        int x = mc.getWindow().getGuiScaledWidth()/2 - 90, y = mc.getWindow().getGuiScaledHeight()-21;
        for (int slot = 0; slot < 9; slot++) {
            final int iconX = x + slot*20;
            RarityDetector.detect(mc.player.getInventory().getItem(slot)).ifPresent(r -> RarityBadges.draw(g, r, iconX+1, y+1, 6));
        }
    }
    static int storageSize(AbstractContainerScreen<?> screen) {
        if (!(screen.getTitle().getContents() instanceof TranslatableContents title)) return -1;
        if (screen.getMenu() instanceof ChestMenu chest && (title.getKey().equals("container.chest") || title.getKey().equals("container.chestDouble"))) return chest.getRowCount()*9;
        if (screen.getMenu() instanceof ShulkerBoxMenu && title.getKey().equals("container.shulkerBox")) return 27;
        return -1;
    }
    private static void startSort(AbstractContainerScreen<?> screen, RaritySort.Mode mode) {
        var mc = Minecraft.getInstance();
        int count = storageSize(screen);
        if (count < 1 || mc.player == null || !screen.getMenu().getCarried().isEmpty()) return;
        var entries = new ArrayList<RaritySort.Entry>();
        expected = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            var slot = screen.getMenu().getSlot(i);
            ItemStack stack = slot.getItem(); expected[i] = stack.copy();
            entries.add(new RaritySort.Entry(stack.isEmpty(), RarityDetector.detect(stack).map(r -> 6-r.ordinal()).orElse(-1),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getHoverName().getString()));
        }
        var order = RaritySort.targetOrder(entries, mode, ascending);
        desired = new ItemStack[count];
        for (int i = 0; i < count; i++) desired[i] = expected[order.get(i)].copy();
        ascending = !ascending; sorting = screen; nextSwap = 0;
    }
    private static void tickSort(Minecraft mc, MinepieceQolClient mod) {
        if (sorting == null) return;
        if (!mod.getConfig().inventorySortingEnabled || mc.player == null || mc.gameMode == null || mc.gui.screen() != sorting || !sorting.getMenu().getCarried().isEmpty()) { sorting = null; return; }
        if (System.currentTimeMillis() < nextSwap) return;
        var menu = sorting.getMenu();
        for (int i = 0; i < expected.length; i++) if (!ItemStack.matches(menu.getSlot(i).getItem(), expected[i])) { sorting = null; return; }
        for (int i = 0; i < expected.length; i++) {
            if (ItemStack.matches(expected[i], desired[i])) continue;
            int j = i+1;
            while (j < expected.length && !ItemStack.matches(expected[j], desired[i])) j++;
            if (j >= expected.length) { sorting = null; return; }
            var first = menu.getSlot(i); var second = menu.getSlot(j);
            if (!first.mayPickup(mc.player) || !second.mayPickup(mc.player) || !first.mayPlace(expected[j]) || !second.mayPlace(expected[i])) { sorting = null; return; }
            // Stable ordering never needs to merge identical stacks; reject rather than alter their quantities.
            if (ItemStack.isSameItemSameComponents(expected[i], expected[j])) { sorting = null; return; }
            mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.PICKUP, mc.player);
            mc.gameMode.handleContainerInput(menu.containerId, j, 0, ContainerInput.PICKUP, mc.player);
            mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.PICKUP, mc.player);
            var swap = expected[i]; expected[i] = expected[j]; expected[j] = swap;
            nextSwap = System.currentTimeMillis()+150;
            return;
        }
        sorting = null;
    }
}
