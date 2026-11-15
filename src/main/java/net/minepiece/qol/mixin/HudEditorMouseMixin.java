package net.minepiece.qol.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minepiece.qol.MinepieceQolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class HudEditorMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void minepiece$hudShortcut(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null && window == Minecraft.getInstance().getWindow().handle()
            && mod.handleHudEditorShortcut(InputConstants.Type.MOUSE.getOrCreate(button.button()), action)) ci.cancel();
    }
}
