package net.minepiece.qol.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minepiece.qol.MinepieceQolClient;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenDrawSlotMixin extends Screen {
    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    @Final
    protected int imageWidth;

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Unique
    private static final long MINEPIECE_BOSS_HOVER_CAPTURE_INTERVAL_MS = 500L;

    @Unique
    private Slot minepiece$lastBossHoverSlot;

    @Unique
    private ItemStack minepiece$lastBossHoverStack = ItemStack.EMPTY;

    @Unique
    private long minepiece$lastBossHoverCaptureMs;

    protected HandledScreenDrawSlotMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void minepiece$drawAuctionTint(GuiGraphicsExtractor drawContext, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || slot == null || !slot.hasItem()) {
            return;
        }

        mod.renderAuctionTint(drawContext, slot.x, slot.y, this.leftPos, this.topPos, slot);
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void minepiece$drawRarityIcon(GuiGraphicsExtractor drawContext, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || slot == null || !slot.hasItem()) {
            return;
        }

        net.minepiece.qol.ui.RarityInventoryTools.dimSlot(mod, drawContext, slot.getItem(), slot.x, slot.y);
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

    @Inject(method = "extractContents", at = @At("RETURN"))
    private void minepiece$inventoryTools(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var mod = MinepieceQolClient.get();
        net.minepiece.qol.ui.RarityInventoryTools.draw(mod, (AbstractContainerScreen<?>)(Object)this, graphics,
            this.leftPos, this.topPos, this.imageWidth, mouseX, mouseY);
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"))
    private void minepiece$captureHoveredBoss(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, CallbackInfo ci) {
        MinepieceQolClient mod = MinepieceQolClient.get();
        if (mod == null || this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
            return;
        }
        boolean cookingEnabled = mod.getConfig().modEnabled && mod.getConfig().cookingHudEnabled;
        if (!mod.isBossTooltipCaptureEnabled() && !cookingEnabled) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        ItemStack stack = this.hoveredSlot.getItem();
        long now = System.currentTimeMillis();
        if (this.hoveredSlot == this.minepiece$lastBossHoverSlot
            && ItemStack.matches(stack, this.minepiece$lastBossHoverStack)
            && now - this.minepiece$lastBossHoverCaptureMs < MINEPIECE_BOSS_HOVER_CAPTURE_INTERVAL_MS) {
            return;
        }
        this.minepiece$lastBossHoverSlot = this.hoveredSlot;
        this.minepiece$lastBossHoverStack = stack.copy();
        this.minepiece$lastBossHoverCaptureMs = now;
        var tooltip = this.getTooltipFromItem(client, stack);
        if (mod.isBossTooltipCaptureEnabled()) {
            mod.captureBossFromTooltipLines(stack, tooltip);
        }
        if (cookingEnabled) {
            mod.getCookingTracker().captureRecipe(stack.getHoverName().getString(),
                net.minepiece.qol.util.TextUtil.normalizeLines(tooltip));
        }
    }
}
