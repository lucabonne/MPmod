package net.minepiece.qol.mixin;

import net.minepiece.qol.MinepieceQolClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public final class ClientPlayerInteractionManagerMixin {
    @Inject(method = "clickSlot", at = @At("HEAD"))
    private void minepiece$onClickSlot(int syncId, int slotId, int button, SlotActionType actionType,
                                      PlayerEntity player, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null && mod.getTelemetryManager() != null) {
            mod.getTelemetryManager().onSlotClick(syncId, slotId, actionType);
        }
    }
}
