package net.minepiece.qol.mixin;

import net.minepiece.qol.MinepieceQolClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private static final long MINEPIECE_BOSS_HOVER_CAPTURE_INTERVAL_MS = 500L;

    @Unique
    private Slot minepiece$lastBossHoverSlot;

    @Unique
    private ItemStack minepiece$lastBossHoverStack = ItemStack.EMPTY;

    @Unique
    private long minepiece$lastBossHoverCaptureMs;

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

        if (!mod.isSlotIconRenderEnabled()) {
            return;
        }

        mod.renderRarityIcon(
            drawContext,
            slot.x,
            slot.y,
            slot,
            null
        );
        mod.renderPetStatIcons(drawContext, slot.x, slot.y, slot, null);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"))
    private void minepiece$captureHoveredBoss(DrawContext drawContext, int mouseX, int mouseY, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || this.focusedSlot == null || !this.focusedSlot.hasStack()) {
            return;
        }
        if (!mod.isBossTooltipCaptureEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        ItemStack stack = this.focusedSlot.getStack();
        long now = System.currentTimeMillis();
        if (this.focusedSlot == this.minepiece$lastBossHoverSlot
            && ItemStack.areEqual(stack, this.minepiece$lastBossHoverStack)
            && now - this.minepiece$lastBossHoverCaptureMs < MINEPIECE_BOSS_HOVER_CAPTURE_INTERVAL_MS) {
            return;
        }
        this.minepiece$lastBossHoverSlot = this.focusedSlot;
        this.minepiece$lastBossHoverStack = stack.copy();
        this.minepiece$lastBossHoverCaptureMs = now;
        mod.captureBossFromTooltipLines(stack, this.getTooltipFromItem(client, stack));
    }
}
