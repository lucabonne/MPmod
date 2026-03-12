package net.minepiece.qol.ui;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.state.BossTracker;
import net.minepiece.qol.util.NumberParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class MinibossWaypointWorldRenderer {
    private static final float TIMER_TARGET_SCREEN_SCALE = 2.2F;
    private static final float TIMER_MIN_WORLD_SCALE = 0.03F;
    private static final int[] COLORS = {
        0xFFAA00,
        0x55CCFF,
        0xFF66AA,
        0x66FF66,
        0xFFFF66,
        0xAA88FF
    };

    private final MinepieceQolClient mod;

    public MinibossWaypointWorldRenderer(MinepieceQolClient mod) {
        this.mod = mod;
    }

    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::render);
    }

    private void render(WorldRenderContext context) {
        if (!this.mod.getConfig().allFeaturesVisible) {
            return;
        }
        if (context.matrixStack() == null || context.consumers() == null || context.camera() == null || context.world() == null) {
            return;
        }

        List<BossTracker.MinibossWaypoint> waypoints = this.mod.getBossTracker().getActiveMinibossWaypoints();
        if (waypoints.isEmpty()) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        Vec3d cameraPos = context.camera().getPos();
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        float tickDelta = context.tickCounter() == null ? 0.0F : context.tickCounter().getTickProgress(true);
        long worldTime = context.world().getTime();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        int index = 0;
        for (BossTracker.MinibossWaypoint waypoint : waypoints) {
            int rgb = colorFor(waypoint.symbol(), index);
            float red = ((rgb >> 16) & 0xFF) / 255.0F;
            float green = ((rgb >> 8) & 0xFF) / 255.0F;
            float blue = (rgb & 0xFF) / 255.0F;

            double x = waypoint.x();
            double y = waypoint.y();
            double z = waypoint.z();

            VertexConsumer fill = consumers.getBuffer(RenderLayer.getDebugFilledBox());
            VertexRendering.drawFilledBox(matrices, fill, x, y, z, x + 1.0D, y + 1.0D, z + 1.0D, red, green, blue, 0.18F);

            VertexConsumer outline = consumers.getBuffer(RenderLayer.getLines());
            VertexRendering.drawBox(matrices, outline, new Box(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D), red, green, blue, 0.95F);

            matrices.push();
            matrices.translate(x, y, z);
            BeaconBlockEntityRenderer.renderBeam(
                matrices,
                consumers,
                BeaconBlockEntityRenderer.BEAM_TEXTURE,
                tickDelta,
                1.0F,
                worldTime,
                0,
                256,
                rgb,
                0.18F,
                0.24F
            );
            matrices.pop();

            String timer = waypoint.remainingMs() <= 0L ? "READY" : NumberParser.formatTimer(waypoint.remainingMs());
            Text timerText = Text.literal(timer);
            double dx = (x + 0.5D) - cameraPos.x;
            double dy = (y + 1.02D) - cameraPos.y;
            double dz = (z + 0.5D) - cameraPos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double fovDeg = client.options.getFov().getValue();
            double focalLength = (client.getWindow().getScaledWidth() / 2.0D) / Math.tan(Math.toRadians(fovDeg) * 0.5D);
            // Keep label roughly constant on-screen size regardless of distance/FOV.
            float textScale = (float) (TIMER_TARGET_SCREEN_SCALE * distance / Math.max(1.0D, focalLength));
            if (textScale < TIMER_MIN_WORLD_SCALE) {
                textScale = TIMER_MIN_WORLD_SCALE;
            }
            matrices.push();
            matrices.translate(x + 0.5D, y + 1.02D, z + 0.5D);
            matrices.multiply(client.getEntityRenderDispatcher().getRotation());
            matrices.scale(textScale, -textScale, textScale);
            float textX = -textRenderer.getWidth(timerText) / 2.0F;
            int bgColor = (int) (client.options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;
            int textColor = 0xFF000000 | rgb;
            textRenderer.draw(
                timerText,
                textX,
                0.0F,
                textColor,
                false,
                matrices.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.SEE_THROUGH,
                bgColor,
                0x00F000F0
            );
            textRenderer.draw(
                timerText,
                textX,
                0.0F,
                textColor,
                false,
                matrices.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0x00000000,
                LightmapTextureManager.applyEmission(0x00F000F0, 2)
            );
            matrices.pop();
            index++;
        }

        matrices.pop();
    }

    private static int colorFor(String symbol, int index) {
        int seed = (symbol == null || symbol.isBlank()) ? index : symbol.hashCode();
        int slot = Math.floorMod(seed, COLORS.length);
        return COLORS[slot];
    }
}
