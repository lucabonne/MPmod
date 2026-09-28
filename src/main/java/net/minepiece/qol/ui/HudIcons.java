package net.minepiece.qol.ui;

import java.util.ArrayList;
import java.util.List;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.state.BossTracker;
import net.minepiece.qol.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class HudIcons {
    private HudIcons() { }

    public static ItemStack findInventoryItem(String name) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && TextUtil.normalize(stack.getName()).equalsIgnoreCase(name)) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    public static List<IconGridTooltipComponent.Row> rows(MinepieceQolClient mod, String title, List<BossTracker.HudLine> lines) {
        int id = 0;
        for (int i = 1; i <= 11; i++) if (mod.getHudPanelName(i).equals(title)) { id = i; break; }
        ItemStack header = switch (id) {
            case 1 -> new ItemStack(Items.IRON_PICKAXE);
            case 2 -> new ItemStack(Items.GOLD_NUGGET);
            case 3, 9 -> new ItemStack(Items.PLAYER_HEAD);
            case 4, 5 -> new ItemStack(Items.IRON_SWORD);
            case 8 -> new ItemStack(Items.PAPER);
            case 10 -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case 11 -> new ItemStack(Items.BOWL);
            default -> ItemStack.EMPTY;
        };
        int profileRows = id == 9 && mod.getConfig().profileXpHudEnabled
            ? mod.getProgressHudController().profile().getHudLines(mod::tr).size() : 0;
        List<ItemStack> xpIcons = id == 9 && mod.getConfig().inventoryXpHudEnabled ? mod.getInventoryXpTracker().getIcons() : List.of();
        List<IconGridTooltipComponent.Row> rows = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            ItemStack icon = i == 0 ? header : ItemStack.EMPTY;
            if (id == 9 && i >= profileRows && i - profileRows < xpIcons.size()) icon = xpIcons.get(i - profileRows);
            if (id == 11 && i > 0) {
                int end = line.text().indexOf(" (");
                if (end > 0) icon = findInventoryItem(line.text().substring(0, end));
            }
            rows.add(new IconGridTooltipComponent.Row(line.text(), line.color(), icon, null));
        }
        return rows;
    }
}
