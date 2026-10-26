package dev.voidclient.modules.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos ins;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * ESP drawing for Minecraft 26.2 using the official extract → submit path.
 *
 * Pattern follows Fabric docs "Rendering in the World":
 * StagedVertexBuffer + custom no-depth RenderPipeline.
 */
public final class VoidWorldRenderer {

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED =
            new StagedVertexBuffer(() -> "VoidClient ESP Buffer", RenderType.SMALL_BUFFER_SIZE);

    private VoidWorldRenderer() {}

    public static void render(LevelRenderContext context) {
        if (VoidClient.MODULE_MANAGER == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        RenderPipeline pipeline = VoidRenderLayers.ESP_LINES_PIPELINE;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) return;

        StagedVertexBuffer.Draw draw = STAGED.appendDraw(
                formatBinding,
                pipeline.getPrimitiveTopology(),
                null
        );

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f pose = matrices.last().pose();

        VertexConsumer builder = STAGED.getVertexBuilder(draw);

        // Partial tick: use current entity positions (stable); full lerp when xo available
        float partialTick = 1.0f;

        Module blockMod = VoidClient.MODULE_MANAGER.getByName("BlockESP");
        if (blockMod instanceof BlockESP blockEsp && blockEsp.isEnabled()) {
            int color = blockEsp.getEspColor();
            boolean tracers = blockEsp.shouldDrawTracers();
            for (BlockPos pos : blockEsp.getFoundBlocks()) {
                boxEdges(builder, pose,
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                        color);
                if (tracers) {
                    tracer(builder, pose, camera,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            color);
                }
            }
        }

        Module playerMod = VoidClient.MODULE_MANAGER.getByName("PlayerESP");
        if (playerMod instanceof PlayerESP playerEsp && playerEsp.isEnabled()) {
            int color = playerEsp.getEspColor();
            for (AbstractClientPlayer other : playerEsp.getTargets()) {
                double x = lerpX(other, partialTick);
                double y = lerpY(other, partialTick);
                double z = lerpZ(other, partialTick);
                float hw = other.getBbWidth() / 2.0f;
                float h = other.getBbHeight();
                boxEdges(builder, pose,
                        x - hw, y, z - hw,
                        x + hw, y + h, z + hw,
                        color);
            }
        }

        Module tracersMod = VoidClient.MODULE_MANAGER.getByName("Tracers");
        if (tracersMod instanceof Tracers tracers && tracers.isEnabled()) {
            for (AbstractClientPlayer other : tracers.getTargets()) {
                double x = lerpX(other, partialTick);
                double y = lerpY(other, partialTick);
                double z = lerpZ(other, partialTick);
                int color = tracers.getTracerColor(other);
                tracer(builder, pose, camera,
                        x, y + other.getBbHeight() * 0.5, z,
                        color);
            }
        }

        matrices.popPose();

        STAGED.upload();
        StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(draw);
        if (info != null) {
            submit(mc, info, pipeline);
        }
        STAGED.endFrame();
    }

    private static double lerpX(AbstractClientPlayer e, float pt) {
        try { return Mth.lerp(pt, e.xo, e.getX()); }
        catch (Throwable t) { return e.getX(); }
    }

    private static double lerpY(AbstractClientPlayer e, float pt) {
        try { return Mth.lerp(pt, e.yo, e.getY()); }
        catch (Throwable t) { return e.getY(); }
    }

    private static double lerpZ(AbstractClientPlayer e, float pt) {
        try { return Mth.lerp(pt, e.zo, e.getZ()); }
        catch (Throwable t) { return e.getZ(); }
    }

    private static void boxEdges(VertexConsumer builder, Matrix4f pose,
                                 double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ,
                                 int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;

        line(builder, pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(builder, pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(builder, pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(builder, pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        line(builder, pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(builder, pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(builder, pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(builder, pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        line(builder, pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(builder, pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(builder, pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(builder, pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void tracer(VertexConsumer builder, Matrix4f pose, Vec3 camera,
                               double tx, double ty, double tz, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;

        Camera cam = Minecraft.getInstance().gameRenderer.mainCamera();
        Vector3fc fwd = cam.forwardVector();

        // Origin = slightly in front of the camera (crosshair), never exactly at eye
        double ox = camera.x + fwd.x() * 0.5;
        double oy = camera.y + fwd.y() * 0.5;
        double oz = camera.z + fwd.z() * 0.5;

        line(builder, pose, ox, oy, oz, tx, ty, tz, r, g, b, a);
    }

    private static void line(VertexConsumer builder, Matrix4f pose,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             float r, float g, float b, float a) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6f) return;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        builder.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);
        builder.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);
    }

    private static void submit(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) return;

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "voidclient_esp_lines",
                        colorTexture,
                        Optional.empty(),
                        mainTarget.getDepthTextureView(),
                        OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    /** Call from GameRenderer#close if resources need explicit release. */
    public static void close() {
        STAGED.close();
    }
}
