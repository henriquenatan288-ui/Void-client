package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Draws persistent 3D boxes and camera-origin tracers in Fabric's world render pass. */
public final class VoidWorldRenderer {
    private static final int MAX_BLOCK_BOXES = 160;
    private VoidWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        VertexConsumer vertices = context.consumers().getBuffer(RenderLayers.lines());

        Module playerEspModule = VoidClient.MODULE_MANAGER == null ? null
                : VoidClient.MODULE_MANAGER.getByName("PlayerESP");
        if (playerEspModule instanceof PlayerESP playerEsp && playerEsp.isEnabled()) {
            int color = playerEsp.getEspColor();
            for (AbstractClientPlayerEntity target : playerEsp.getTargets()) {
                drawBox(vertices, target.getBoundingBox(), cameraPos, color);
            }
        }

        Module tracerModule = VoidClient.MODULE_MANAGER == null ? null
                : VoidClient.MODULE_MANAGER.getByName("Tracers");
        if (tracerModule instanceof Tracers tracers && tracers.isEnabled()) {
            int color = tracers.getTracerColor();
            for (AbstractClientPlayerEntity target : tracers.getTargets()) {
                drawLine(vertices, 0, 0, 0,
                        target.getX() - cameraPos.x,
                        target.getBodyY(0.55) - cameraPos.y,
                        target.getZ() - cameraPos.z, color);
            }
        }

        Module blockEspModule = VoidClient.MODULE_MANAGER == null ? null
                : VoidClient.MODULE_MANAGER.getByName("BlockESP");
        if (blockEspModule instanceof BlockESP blockEsp && blockEsp.isEnabled()) {
            int color = blockEsp.getEspColor();
            int rendered = 0;
            for (BlockPos pos : blockEsp.getFoundBlocks()) {
                if (rendered++ >= MAX_BLOCK_BOXES) break;
                drawBox(vertices,
                        pos.getX() + 0.025, pos.getY() + 0.025, pos.getZ() + 0.025,
                        pos.getX() + 0.975, pos.getY() + 0.975, pos.getZ() + 0.975,
                        cameraPos, color);
                if (blockEsp.shouldDrawTracers()) {
                    drawLine(vertices, 0, 0, 0,
                            pos.getX() + 0.5 - cameraPos.x,
                            pos.getY() + 0.5 - cameraPos.y,
                            pos.getZ() + 0.5 - cameraPos.z, color);
                }
            }
        }
    }

    private static void drawBox(VertexConsumer v, Box b, Vec3d camera, int color) {
        drawBox(v, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, camera, color);
    }

    private static void drawBox(VertexConsumer v,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ, Vec3d camera, int color) {
        double x0 = minX - camera.x, x1 = maxX - camera.x;
        double y0 = minY - camera.y, y1 = maxY - camera.y;
        double z0 = minZ - camera.z, z1 = maxZ - camera.z;
        drawLine(v, x0,y0,z0, x1,y0,z0, color);
        drawLine(v, x0,y0,z1, x1,y0,z1, color);
        drawLine(v, x0,y1,z0, x1,y1,z0, color);
        drawLine(v, x0,y1,z1, x1,y1,z1, color);
        drawLine(v, x0,y0,z0, x0,y1,z0, color);
        drawLine(v, x1,y0,z0, x1,y1,z0, color);
        drawLine(v, x0,y0,z1, x0,y1,z1, color);
        drawLine(v, x1,y0,z1, x1,y1,z1, color);
        drawLine(v, x0,y0,z0, x0,y0,z1, color);
        drawLine(v, x1,y0,z0, x1,y0,z1, color);
        drawLine(v, x0,y1,z0, x0,y1,z1, color);
        drawLine(v, x1,y1,z0, x1,y1,z1, color);
    }

    private static void drawLine(VertexConsumer vertices,
            double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        int alpha = (argb >>> 24) & 0xFF;
        vertex(vertices, x1, y1, z1, red, green, blue, alpha);
        vertex(vertices, x2, y2, z2, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer v, double x, double y, double z,
                               int red, int green, int blue, int alpha) {
        v.vertex((float)x, (float)y, (float)z)
                .color(red, green, blue, alpha)
                .normal(0.0F, 1.0F, 0.0F);
    }
}
