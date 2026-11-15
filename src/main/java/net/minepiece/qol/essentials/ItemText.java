package net.minepiece.qol.essentials;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Texte serveur d'une pile sans construire le tooltip complet
 * ({@code getTooltipLines} déclenche tous les callbacks de tooltip du mod et
 * leur sérialisation NBT — beaucoup trop cher pour un scan périodique).
 */
public final class ItemText {
    private ItemText() {}

    /** Lignes du composant LORE, sous forme de String. */
    public static List<String> lore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return List.of();
        List<String> out = new ArrayList<>(lore.lines().size());
        for (Component line : lore.lines()) out.add(line.getString());
        return out;
    }

    /** Nom affiché puis lore : la forme « tooltip » sur laquelle les parseurs ont été écrits. */
    public static List<String> nameAndLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        List<String> out = new ArrayList<>(1 + (lore == null ? 0 : lore.lines().size()));
        out.add(stack.getHoverName().getString());
        if (lore != null) for (Component line : lore.lines()) out.add(line.getString());
        return out;
    }
}
