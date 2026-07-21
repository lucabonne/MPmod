package net.minepiece.qol.mixin;

import net.minepiece.qol.MinepieceQolClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public final class InGameHudMixin {
    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void minepiece$onOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null) {
            mod.handleActionbarMessage(message);
        }
    }

    @Inject(method = "setTitle(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void minepiece$onTitle(Text title, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null) {
            mod.handleTitle(title);
        }
    }

    @Inject(method = "setSubtitle(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void minepiece$onSubtitle(Text subtitle, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null) {
            mod.handleTitle(subtitle);
        }
    }
}
