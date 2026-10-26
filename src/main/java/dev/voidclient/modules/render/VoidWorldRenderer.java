package dev.voidclient.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws ESP boxes and tracers in the 26.2 world render path.
 *
 * Uses LevelRenderEvents.AFTER_SOLID_FEATURES + the custom no-depth
 * line pipeline registered in VoidRenderLayers.
 */
public final class VoidWorldRenderer {

    private VoidWorldRenderer() {}

    public static void render(LevelRenderContext context) {
        if (VoidClient.MODULE_MANAGER == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        float partialTick = context.levelState().cameraRenderState.cameraEntityPartialTicks;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        Matrix4f pose = matrices.last().pose();

        // --- BlockESP boxes + optional tracers ---
        Module blockMod = VoidClient.MODULE_MANAGER.getByName("BlockESP");
        if (blockMod instanceof BlockESP blockEsp && blockEsp.isEnabled()) {
            List<BlockPos> blocks = blockEsp.getFoundBlocks();
            int color = blockEsp.getEspColor();
            boolean drawTracers = blockEsp.shouldDrawTracers();

            for (BlockPos pos : blocks) {
                drawBoxEdges(pose, pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, color);

                if (drawTracers) {
                    double cx = pos.getX() + 0.5;
                    double cy = pos.getY() + 0.5;
                    double cz = pos.getZ() + 0.5;
                    drawTracer(pose, camera, cx, cy, cz, color);
                }
            }
        }

        // --- PlayerESP boxes ---
        Module playerMod = VoidClient.MODULE_MANAGER.getByName("PlayerESP");
        if (playerMod instanceof PlayerESP playerEsp && playerEsp.isEnabled()) {
            int color = playerEsp.getEspColor();
            for (AbstractClientPlayer other : playerEsp.getTargets()) {
                double x = Mth.lerp(partialTick, other.xo, other.getX());
                double y = Mth.lerp(partialTick, other.yo, other.getY());
                double z = Mth.lerp(partialTick, other.zo, other.getZ());

                float halfWidth = other.getBbWidth() / 2.0f;
                float height = other.getBbHeight();

                drawBoxEdges(pose,
                        x - halfWidth, y, z - halfWidth,
                        x + halfWidth, y + height, z + halfWidth,
                        color);
            }
        }

        // --- Tracers module ---
        Module tracersMod = VoidClient.MODULE_MANAGER.getByName("Tracers");
        if (tracersMod instanceof Tracers tracers && tracers.isEnabled()) {
            for (AbstractClientPlayer other : tracers.getTargets()) {
                double x = Mth.lerp(partialTick, other.xo, other.getX());
                double y = Mth.lerp(partialTick, other.yo, other.getY());
                double z = Mth.lerp(partialTick, other.zo, other.getZ());

                // Target = center of the body
                double tx = x;
                double ty = y + other.getBbHeight() * 0.5;
                double tz = z;

                int color = tracers.getTracerColor(other);
                drawTracer(pose, camera, tx, ty, tz, color);
            }
        }

        matrices.popPose();
    }

    /**
     * Draws the 12 edges of an axis-aligned box using the ESP line pipeline.
     */
    private static void drawBoxEdges(Matrix4f pose,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;

        // Bottom face
        line(pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Top face
        line(pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Vertical edges
        line(pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    /**
     * Tracer from near the crosshair (camera + look * 0.5) to the target point.
     * Origin is slightly in front of the camera so the line is never degenerate in first person.
     */
    private static void drawTracer(Matrix4f pose, Vec3 camera, double tx, double ty, double tz, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;

        Minecraft mc = Minecraft.getInstance();
        Vec3 look = mc.gameRenderer.getMainCamera().getLookVector();

        // Origin = a bit in front of the camera (mira), not exactly at the eye
        double ox = camera.x + look.x * 0.5;
        double oy = camera.y + look.y * 0.5;
        double oz = camera.z + look.z * 0.5;

        // Vertices are already in world space relative to camera because we translated the matrix
        line(pose, ox, oy, oz, tx, ty, tz, r, g, b, a);
    }

    private static void line(Matrix4f pose,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             float r, float g, float b, float a) {
        // Use the custom through-walls line RenderType
        VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource()
                .getBuffer(VoidRenderLayers.ESP_LINES);

        // Normal for the line (direction)
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6f) return;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);
    }
}
