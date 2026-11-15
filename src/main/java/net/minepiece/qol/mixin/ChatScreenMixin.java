package net.minepiece.qol.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.ui.ChatChannelOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow protected EditBox input;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void minepiece$clickIsland(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null && click.button() == 0 && click.y() < this.input.getY() - 2
            && mod.toggleBossIslandAt(click.x(), click.y())) cir.setReturnValue(true);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void minepiece$renderChannel(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChatChannelOverlay.render(context, this.input.getX(), this.input.getY());
    }
}
