package net.minepiece.qol.ui;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.state.BossTracker;
import net.minepiece.qol.util.NumberParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

public final class MinibossWaypointWorldRenderer {
    private static final float TIMER_TARGET_SCREEN_SCALE = 2.2F;
    private static final float TIMER_MIN_WORLD_SCALE = 0.03F;
    private static final int BOSS_WAYPOINT_COLOR = 0x6FA8FF;
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
        if (!this.mod.getConfig().modEnabled || !this.mod.getConfig().allFeaturesVisible) {
            return;
        }
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        OrderedRenderCommandQueue commandQueue = context.commandQueue();
        WorldRenderState worldState = context.worldState();
        if (matrices == null || consumers == null || commandQueue == null || worldState == null) {
            return;
        }
        CameraRenderState cameraState = worldState.cameraRenderState;
        if (cameraState == null || cameraState.pos == null) {
            return;
        }

        List<BossTracker.MinibossWaypoint> minibossWaypoints = this.mod.getBossTracker().getActiveMinibossWaypoints();
        List<BossTracker.BossWaypoint> bossWaypoints = this.mod.getBossTracker().getActiveBossWaypoints();
        if (minibossWaypoints.isEmpty() && bossWaypoints.isEmpty()) {
            return;
        }

        Vec3d cameraPos = cameraState.pos;
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        float tickDelta = client.getRenderTickCounter().getTickProgress(true);

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        int index = 0;
        for (BossTracker.MinibossWaypoint waypoint : minibossWaypoints) {
            int rgb = colorFor(waypoint.symbol(), index);
            drawWaypoint(
                matrices,
                consumers,
                commandQueue,
                textRenderer,
                client,
                cameraPos,
                cameraState,
                tickDelta,
                waypoint.x(),
                waypoint.y(),
                waypoint.z(),
                waypoint.remainingMs(),
                rgb
            );
            index++;
        }

        for (BossTracker.BossWaypoint waypoint : bossWaypoints) {
            drawWaypoint(
                matrices,
                consumers,
                commandQueue,
                textRenderer,
                client,
                cameraPos,
                cameraState,
                tickDelta,
                waypoint.x(),
                waypoint.y(),
                waypoint.z(),
                waypoint.remainingMs(),
                BOSS_WAYPOINT_COLOR
            );
        }

        matrices.pop();
    }

    private static void drawWaypoint(
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        OrderedRenderCommandQueue commandQueue,
        TextRenderer textRenderer,
        MinecraftClient client,
        Vec3d cameraPos,
        CameraRenderState cameraState,
        float tickDelta,
        int xBlock,
        int yBlock,
        int zBlock,
        long remainingMs,
        int rgb
    ) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        double x = xBlock;
        double y = yBlock;
        double z = zBlock;

        // Draw block outline
        int outlineColor = (0xFF << 24) | rgb;
        VertexConsumer outline = consumers.getBuffer(RenderLayers.lines());
        VertexRendering.drawOutline(matrices, outline, VoxelShapes.fullCube(), x, y, z, outlineColor, 1.0F);

        // Beacon beam
        matrices.push();
        matrices.translate(x, y, z);
        BeaconBlockEntityRenderer.renderBeam(
            matrices,
            commandQueue,
            BeaconBlockEntityRenderer.BEAM_TEXTURE,
            tickDelta,
            1.0F,
            0,
            256,
            rgb,
            0.18F,
            0.24F
        );
        matrices.pop();

        // Timer text billboard
        String timer = remainingMs <= 0L ? "READY" : NumberParser.formatTimer(remainingMs);
        Text timerText = Text.literal(timer);
        double dx = (x + 0.5D) - cameraPos.x;
        double dy = (y + 1.02D) - cameraPos.y;
        double dz = (z + 0.5D) - cameraPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double fovDeg = client.options.getFov().getValue();
        double focalLength = (client.getWindow().getScaledWidth() / 2.0D) / Math.tan(Math.toRadians(fovDeg) * 0.5D);
        float textScale = (float) (TIMER_TARGET_SCREEN_SCALE * distance / Math.max(1.0D, focalLength));
        if (textScale < TIMER_MIN_WORLD_SCALE) {
            textScale = TIMER_MIN_WORLD_SCALE;
        }
        matrices.push();
        matrices.translate(x + 0.5D, y + 1.02D, z + 0.5D);
        matrices.multiply(cameraState.orientation);
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
    }

    private static int colorFor(String symbol, int index) {
        int seed = (symbol == null || symbol.isBlank()) ? index : symbol.hashCode();
        int slot = Math.floorMod(seed, COLORS.length);
        return COLORS[slot];
    }
}
