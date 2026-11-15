package net.minepiece.qol.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.state.BossTracker;
import net.minepiece.qol.util.NumberParser;

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
        LevelRenderEvents.COLLECT_SUBMITS.register(this::render);
    }

    private void render(LevelRenderContext context) {
        if (!this.mod.getConfig().modEnabled || !this.mod.getConfig().allFeaturesVisible) {
            return;
        }
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector commandQueue = context.submitNodeCollector();
        LevelRenderState worldState = context.levelState();
        if (matrices == null || commandQueue == null || worldState == null) {
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

        Vec3 cameraPos = cameraState.pos;
        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;
        float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        int index = 0;
        for (BossTracker.MinibossWaypoint waypoint : minibossWaypoints) {
            int rgb = colorFor(waypoint.symbol(), index);
            drawWaypoint(
                matrices,
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

        matrices.popPose();
    }

    private static void drawWaypoint(
        PoseStack matrices,
        SubmitNodeCollector commandQueue,
        Font textRenderer,
        Minecraft client,
        Vec3 cameraPos,
        CameraRenderState cameraState,
        float tickDelta,
        int xBlock,
        int yBlock,
        int zBlock,
        long remainingMs,
        int rgb
    ) {
        double x = xBlock;
        double y = yBlock;
        double z = zBlock;

        // Draw block outline
        int outlineColor = (0xFF << 24) | rgb;
        matrices.pushPose();
        matrices.translate(x, y, z);
        commandQueue.submitShapeOutline(matrices, Shapes.block(), RenderTypes.lines(), outlineColor, 1.0F, false);
        matrices.popPose();

        // Beacon beam
        matrices.pushPose();
        matrices.translate(x, y, z);
        BeaconRenderer.submitBeaconBeam(
            matrices,
            commandQueue,
            BeaconRenderer.BEAM_LOCATION,
            tickDelta,
            1.0F,
            0,
            256,
            rgb,
            0.18F,
            0.24F
        );
        matrices.popPose();

        // Timer text billboard
        String timer = remainingMs <= 0L ? "READY" : NumberParser.formatTimer(remainingMs);
        Component timerText = Component.literal(timer);
        double dx = (x + 0.5D) - cameraPos.x;
        double dy = (y + 1.02D) - cameraPos.y;
        double dz = (z + 0.5D) - cameraPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double fovDeg = client.options.fov().get();
        double focalLength = (client.getWindow().getGuiScaledWidth() / 2.0D) / Math.tan(Math.toRadians(fovDeg) * 0.5D);
        float textScale = (float) (TIMER_TARGET_SCREEN_SCALE * distance / Math.max(1.0D, focalLength));
        if (textScale < TIMER_MIN_WORLD_SCALE) {
            textScale = TIMER_MIN_WORLD_SCALE;
        }
        matrices.pushPose();
        matrices.translate(x + 0.5D, y + 1.02D, z + 0.5D);
        matrices.mulPose(cameraState.orientation);
        matrices.scale(textScale, -textScale, textScale);
        float textX = -textRenderer.width(timerText) / 2.0F;
        int bgColor = (int) (client.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        int textColor = 0xFF000000 | rgb;
        commandQueue.submitText(matrices, textX, 0.0F, timerText.getVisualOrderText(), false,
            Font.DisplayMode.SEE_THROUGH, 0x00F000F0, textColor, bgColor, 0);
        commandQueue.submitText(matrices, textX, 0.0F, timerText.getVisualOrderText(), false,
            Font.DisplayMode.NORMAL, 0x00F000F0, textColor, 0, 0);
        matrices.popPose();
    }

    private static int colorFor(String symbol, int index) {
        int seed = (symbol == null || symbol.isBlank()) ? index : symbol.hashCode();
        int slot = Math.floorMod(seed, COLORS.length);
        return COLORS[slot];
    }
}
