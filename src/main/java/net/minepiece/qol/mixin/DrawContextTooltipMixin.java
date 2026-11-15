package net.minepiece.qol.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minepiece.qol.ui.ItemDetailTooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphicsExtractor.class)
public abstract class DrawContextTooltipMixin {
    @ModifyVariable(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<Component> minepiece$gridRows(List<Component> lines, Font renderer, List<Component> original,
                                         Optional<TooltipComponent> data, int x, int y, Identifier texture) {
        if (data.orElse(null) instanceof ItemDetailTooltipData detail) {
            List<Component> display = new ArrayList<>(lines);
            ItemDetailTooltipData.removeReplacedLines(display, detail.hasIngredients());
            return display;
        }
        return lines;
    }
}
