package net.minepiece.qol.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import net.minepiece.qol.MinepieceQolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public final class BackgroundMenuMixin {
    private static net.minepiece.qol.essentials.EssentialsFeatures features() {
        var mod = MinepieceQolClient.get();
        return Minecraft.getInstance().isSameThread() && mod != null ? mod.getEssentials() : null;
    }
    @Inject(method = "handleTabListCustomisation", at = @At("HEAD"))
    private void minepiece$tab(ClientboundTabListPacket packet, CallbackInfo ci) {
        var f = features();
        if (f != null) f.observeServer(packet.header().getString() + packet.footer().getString());
    }
    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void minepiece$open(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        var f = features(); if (f != null && f.background().onOpen(packet)) ci.cancel();
    }
    @Inject(method = "handleContainerContent", at = @At("HEAD"), cancellable = true)
    private void minepiece$content(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        var f = features(); if (f != null && f.background().onContent(packet)) ci.cancel();
    }
    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"), cancellable = true)
    private void minepiece$slot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        var f = features(); if (f != null && f.background().onSlot(packet)) ci.cancel();
    }
    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    private void minepiece$close(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        var f = features(); if (f != null && f.background().onClose(packet.getContainerId())) ci.cancel();
    }
}
