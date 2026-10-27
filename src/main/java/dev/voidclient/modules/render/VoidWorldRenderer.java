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
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class VoidWorldRenderer {

    private static final float LINE_WIDTH = 2.2f;
    private static final float TRACER_FORWARD = 0.75f;
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED =
            new StagedVertexBuffer(() -> "VoidClient ESP Buffer", RenderType.SMALL_BUFFER_SIZE);
    private static boolean loggedPipelineFail;
    private static long lastRenderFailNs;

    private VoidWorldRenderer() {}

    public static void render(LevelRenderContext context) {
        if (VoidClient.MODULE_MANAGER == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        RenderPipeline pipeline = VoidRenderLayers.ESP_LINES_PIPELINE;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) {
            if (!loggedPipelineFail) {
                loggedPipelineFail = true;
                VoidClient.LOGGER.warn("ESP pipeline has no vertex format binding — lines disabled");
            }
            return;
        }

        PoseStack matrices = context.poseStack();
        boolean pushed = false;
        try {
            StagedVertexBuffer.Draw draw = STAGED.appendDraw(
                    formatBinding, pipeline.getPrimitiveTopology(), null);

            Camera cam = mc.gameRenderer.mainCamera();
            Vec3 cameraPos = cam.position();

            matrices.pushPose();
            pushed = true;
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Matrix4f pose = matrices.last().pose();
            VertexConsumer builder = STAGED.getVertexBuilder(draw);
            float partialTick = 1.0f;
            try {
                partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            } catch (Throwable ignored) {}

            Module blockMod = VoidClient.MODULE_MANAGER.getByName("BlockESP");
            if (blockMod instanceof BlockESP blockEsp && blockEsp.isEnabled()) {
                int color = blockEsp.getEspColor();
                boolean tracers = blockEsp.shouldDrawTracers();
                for (BlockPos pos : blockEsp.getFoundBlocks()) {
                    boxEdges(builder, pose,
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, color);
                    if (tracers) {
                        tracer(builder, pose, cam,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, color);
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
                    boxEdges(builder, pose, x - hw, y, z - hw, x + hw, y + h, z + hw, color);
                }
            }

            Module tracersMod = VoidClient.MODULE_MANAGER.getByName("Tracers");
            if (tracersMod instanceof Tracers tracers && tracers.isEnabled()) {
                for (AbstractClientPlayer other : tracers.getTargets()) {
                    double x = lerpX(other, partialTick);
                    double y = lerpY(other, partialTick);
                    double z = lerpZ(other, partialTick);
                    tracer(builder, pose, cam, x, y + other.getBbHeight() * 0.5, z,
                            tracers.getTracerColor(other));
                }
            }

            Module wavesMod = VoidClient.MODULE_MANAGER.getByName("BreakWaves");
            if (wavesMod instanceof BreakWaves breakWaves && breakWaves.isEnabled()) {
                int baseColor = breakWaves.getWaveColor();
                int ringCount = breakWaves.getWaveCount();
                int maxAge = Math.max(1, breakWaves.getMaxAge());
                long now = mc.level.getGameTime();
                for (BreakWaves.Wave wave : breakWaves.getActiveWaves()) {
                    for (int ring = 0; ring < ringCount; ring++) {
                        float ageF = (now - wave.born) + partialTick - ring * 2.5f;
                        if (ageF < 0 || ageF > maxAge) continue;
                        float t = ageF / maxAge;
                        float fade = (1.0f - t) * (1.0f - t);
                        int baseA = (baseColor >>> 24) & 0xFF;
                        int a = Math.max(18, (int) (baseA * fade));
                        int rgb = baseColor & 0x00FFFFFF;
                        int faded = (a << 24) | rgb;
                        double radius = 0.2 + ageF * 0.32;
                        double yOff = ring * 0.04;
                        ring(builder, pose, wave.x, wave.y + yOff, wave.z, radius, 28, faded);
                        ring(builder, pose, wave.x, wave.y + yOff + 0.06, wave.z, radius * 0.92, 20, faded);
                    }
                }
            }

            matrices.popPose();
            pushed = false;

            STAGED.upload();
            StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(draw);
            if (info != null) submit(mc, info, pipeline);
            STAGED.endFrame();
        } catch (Throwable t) {
            if (pushed) {
                try { matrices.popPose(); } catch (Throwable ignored) {}
            }
            long now = System.nanoTime();
            if (now - lastRenderFailNs > 5_000_000_000L) {
                lastRenderFailNs = now;
                VoidClient.LOGGER.error("VoidWorldRenderer frame failed", t);
            }
        }
    }

    private static double lerpX(AbstractClientPlayer e, float pt) {
        try { return Mth.lerp(pt, e.xo, e.getX()); } catch (Throwable t) { return e.getX(); }
    }
    private static double lerpY(AbstractClientPlayer e, float pt) {
        try { return Mth.lerp(pt, e.yo, e.getY()); } catch (Throwable t) { return e.getY(); }
    }
    private static double lerpZ(AbstractClientPlayer e, float pt) {
        try { return Mth.lerp(pt, e.zo, e.getZ()); } catch (Throwable t) { return e.getZ(); }
    }

    private static void ring(VertexConsumer builder, Matrix4f pose,
                             double cx, double cy, double cz,
                             double radius, int segments, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;
        double prevX = cx + radius;
        double prevZ = cz;
        for (int i = 1; i <= segments; i++) {
            double angle = (Math.PI * 2.0) * i / segments;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            line(builder, pose, prevX, cy, prevZ, x, cy, z, r, g, b, a);
            prevX = x;
            prevZ = z;
        }
    }

    private static void boxEdges(VertexConsumer builder, Matrix4f pose,
                                 double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ, int argb) {
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

    private static void tracer(VertexConsumer builder, Matrix4f pose, Camera cam,
                               double tx, double ty, double tz, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;
        Vec3 eye = cam.position();
        Vector3fc fwd = cam.forwardVector();
        double ox = eye.x + fwd.x() * TRACER_FORWARD;
        double oy = eye.y + fwd.y() * TRACER_FORWARD;
        double oz = eye.z + fwd.z() * TRACER_FORWARD;
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
        float nx = dx / len, ny = dy / len, nz = dz / len;
        builder.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
        builder.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
    }

    private static void submit(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) return;
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "voidclient_esp_lines", colorTexture, Optional.empty(),
                mainTarget.getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    public static void close() { STAGED.close(); }
}
