package net.minepiece.qol.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import net.minepiece.qol.ui.AppearanceScreen;
import net.minepiece.qol.ui.HelpScreen;
import net.minepiece.qol.ui.HudLayoutScreen;
import net.minepiece.qol.ui.MinepieceMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class ModWidgetAudioMixin {
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void minepiece$silentModControls(SoundManager manager, CallbackInfo ci) {
        var screen = Minecraft.getInstance().gui.screen();
        if (screen instanceof MinepieceMenuScreen || screen instanceof AppearanceScreen
            || screen instanceof HudLayoutScreen || screen instanceof HelpScreen) ci.cancel();
    }
}
