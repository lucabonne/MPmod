package net.minepiece.qol.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minepiece.qol.MinepieceQolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class HudEditorKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void minepiece$hudShortcut(long window, int action, KeyEvent event, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod != null && window == Minecraft.getInstance().getWindow().handle()
            && mod.handleHudEditorShortcut(InputConstants.getKey(event), action)) ci.cancel();
    }
}
