package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Draws persistent 3D boxes and camera-forward tracers in Fabric's world render pass. */
public final class VoidWorldRenderer {
    private static final int MAX_BLOCK_BOXES = 160;
    private VoidWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        float tickProgress = mc.getRenderTickCounter().getTickProgress(false);
        if (context.consumers() == null) return;
        VertexConsumer vertices = context.consumers().getBuffer(VoidRenderLayers.ESP_LINES);

        Module playerEspModule = VoidClient.MODULE_MANAGER == null ? null
                : VoidClient.MODULE_MANAGER.getByName("PlayerESP");
        if (playerEspModule instanceof PlayerESP playerEsp && playerEsp.isEnabled()) {
            int color = playerEsp.getEspColor();
            for (AbstractClientPlayerEntity target : playerEsp.getTargets()) {
                Vec3d interpolatedPos = target.getLerpedPos(tickProgress);
                Box interpolatedBox = target.getBoundingBox()
                        .offset(interpolatedPos.subtract(target.getEntityPos()));
                drawBox(vertices, interpolatedBox, cameraPos, color);
            }
        }

        Module tracerModule = VoidClient.MODULE_MANAGER == null ? null
                : VoidClient.MODULE_MANAGER.getByName("Tracers");
        if (tracerModule instanceof Tracers tracers && tracers.isEnabled()) {
            for (AbstractClientPlayerEntity target : tracers.getTargets()) {
                int color = tracers.getTracerColor(target);
                Vec3d interpolatedPos = target.getLerpedPos(tickProgress);
                Vec3d tracerStart = cameraForwardOffset(camera, 0.65);
                drawLine(vertices, tracerStart.x, tracerStart.y, tracerStart.z,
                        interpolatedPos.x - cameraPos.x,
                        interpolatedPos.y + target.getHeight() * 0.55 - cameraPos.y,
                        interpolatedPos.z - cameraPos.z, color);
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
                    Vec3d tracerStart = cameraForwardOffset(camera, 0.65);
                    drawLine(vertices, tracerStart.x, tracerStart.y, tracerStart.z,
                            pos.getX() + 0.5 - cameraPos.x,
                            pos.getY() + 0.5 - cameraPos.y,
                            pos.getZ() + 0.5 - cameraPos.z, color);
                }
            }
        }
    }

    private static Vec3d cameraForwardOffset(Camera camera, double distance) {
        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());
        double cosPitch = Math.cos(pitch);
        return new Vec3d(
                -Math.sin(yaw) * cosPitch,
                -Math.sin(pitch),
                Math.cos(yaw) * cosPitch
        ).multiply(distance);
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
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-6) return;
        float nx = (float) (dx / length);
        float ny = (float) (dy / length);
        float nz = (float) (dz / length);
        vertex(vertices, x1, y1, z1, red, green, blue, alpha, nx, ny, nz);
        vertex(vertices, x2, y2, z2, red, green, blue, alpha, nx, ny, nz);
    }

    private static void vertex(VertexConsumer v, double x, double y, double z,
                               int red, int green, int blue, int alpha,
                               float nx, float ny, float nz) {
        v.vertex((float)x, (float)y, (float)z)
                .color(red, green, blue, alpha)
                .normal(nx, ny, nz)
                .lineWidth(1.5F);
    }
}
