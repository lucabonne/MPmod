package net.minepiece.qol.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minepiece.qol.MinepieceQolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public final class InGameHudMixin {
    @Inject(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"))
    private void minepiece$onOverlayMessage(Component message, boolean tinted, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null) {
            mod.handleActionbarMessage(message);
        }
    }

    @Inject(method = "setTitle(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void minepiece$onTitle(Component title, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null) {
            mod.handleTitle(title);
        }
    }

    @Inject(method = "setSubtitle(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void minepiece$onSubtitle(Component subtitle, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null) {
            mod.handleTitle(subtitle);
        }
    }
}
