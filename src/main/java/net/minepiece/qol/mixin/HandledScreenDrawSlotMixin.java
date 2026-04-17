package net.minepiece.qol.mixin;

import net.minepiece.qol.MinepieceQolClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenDrawSlotMixin extends Screen {
    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    protected HandledScreenDrawSlotMixin(Text title) {
        super(title);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void minepiece$drawAuctionTint(DrawContext drawContext, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || slot == null || !slot.hasStack()) {
            return;
        }

        mod.renderAuctionTint(drawContext, slot.x, slot.y, this.x, this.y, slot);
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void minepiece$drawRarityIcon(DrawContext drawContext, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || slot == null || !slot.hasStack()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        ItemStack stack = slot.getStack();
        java.util.List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
        mod.renderRarityIcon(
            drawContext,
            slot.x,
            slot.y,
            slot,
            tooltip
        );
        mod.renderPetStatIcons(drawContext, slot.x, slot.y, slot, tooltip);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"))
    private void minepiece$captureHoveredBoss(DrawContext drawContext, int mouseX, int mouseY, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || this.focusedSlot == null || !this.focusedSlot.hasStack()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        ItemStack stack = this.focusedSlot.getStack();
        mod.captureBossFromTooltipLines(stack, this.getTooltipFromItem(client, stack));
    }
}
