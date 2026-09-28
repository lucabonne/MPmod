package net.minepiece.qol.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minepiece.qol.ui.ItemDetailTooltipData;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public abstract class DrawContextTooltipMixin {
    @ModifyVariable(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<Text> minepiece$gridRows(List<Text> lines, TextRenderer renderer, List<Text> original,
                                         Optional<TooltipData> data, int x, int y, Identifier texture) {
        if (data.orElse(null) instanceof ItemDetailTooltipData detail) {
            List<Text> display = new ArrayList<>(lines);
            ItemDetailTooltipData.removeReplacedLines(display, detail.hasIngredients());
            return display;
        }
        return lines;
    }
}
