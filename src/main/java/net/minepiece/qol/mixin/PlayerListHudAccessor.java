package net.minepiece.qol.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
    @Accessor("footer")
    Text minepiece$getFooter();

    @Accessor("header")
    Text minepiece$getHeader();
}
