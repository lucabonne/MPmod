package net.minepiece.qol.mixin;

import java.util.Optional;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.ui.ItemDetailTooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    @Inject(method = "getTooltipData", at = @At("RETURN"), cancellable = true)
    private void minepiece$details(CallbackInfoReturnable<Optional<TooltipData>> cir) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || !mod.getConfig().modEnabled || !mod.getConfig().allFeaturesVisible
            || !mod.getConfig().appearance.iconRows || cir.getReturnValue().isPresent()) return;
        var data = ItemDetailTooltipData.from((ItemStack) (Object) this, mod);
        if (data != null) cir.setReturnValue(Optional.of(data));
    }
}
