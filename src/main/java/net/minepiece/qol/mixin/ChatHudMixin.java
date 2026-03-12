package net.minepiece.qol.mixin;

import net.minepiece.qol.MinepieceQolClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public final class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void minepiece$onChatMessage(Text message, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null) {
            mod.handleChatMessage(message);
        }
    }
}
