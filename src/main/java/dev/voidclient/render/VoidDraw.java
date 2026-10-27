package dev.voidclient.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.voidclient.freecam.FreecamController;
import dev.voidclient.freelook.FreelookController;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shape pipeline with smooth visual motion:
 * extract + draw every render frame using (gameTime + partialTick).
 * Soft-fail: one bad frame does not kill all visuals permanently.
 */
public final class VoidDraw {
    private VoidDraw() {}

    private static final Logger LOG = LoggerFactory.getLogger("VoidClient/Draw");

    public interface Extractor {
        void extract(Sink sink, Minecraft mc, float partialTick);
    }

    public interface Sink {
        void line(Vec3 a, Vec3 b, float width, boolean screenScale, int argb, boolean xray);
        void box(double x0, double y0, double z0, double x1, double y1, double z1, int argb, boolean xray);
        void tracer(Vec3 target, float width, int argb);
    }

    public static final List<Extractor> EXTRACTORS = new CopyOnWriteArrayList<>();

    private sealed interface Shape permits Seg, Fill, Tr {}
    private record Seg(Vec3 a, Vec3 b, float width, boolean screenScale, int argb, boolean xray) implements Shape {}
    private record Fill(double x0, double y0, double z0, double x1, double y1, double z1, int argb, boolean xray) implements Shape {}
    private record Tr(Vec3 target, float width, int argb) implements Shape {}
    private record Frame(List<Shape> shapes) {}

    private static volatile Frame frame = new Frame(List.of());
    private static int failStreak;
    private static long lastFailLogNs;
    private static final int MAX_FAIL_STREAK = 30;

    private static long smoothEpochNs = System.nanoTime();
    private static long smoothWorldAnchor = Long.MIN_VALUE;

    private static final RenderPipeline XRAY = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("voidclient", "pipeline/shapes_xray"))
                    .withDepthStencilState(Optional.empty())
                    .build());
    private static final RenderPipeline DEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("voidclient", "pipeline/shapes_depth"))
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer BUF =
            new StagedVertexBuffer(() -> "VoidDraw buffer", RenderType.SMALL_BUFFER_SIZE);

    public static void init() {
        // Per-frame extract = smooth spin for Visuals / Particles / CustomSky / WorldFx
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ctx -> {
            extractFresh();
            draw(ctx);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> BUF.close());
        LOG.info("VoidDraw initialized (per-frame extract, soft-fail)");
    }

    /**
     * Real sub-tick time. If delta tracker returns 0, synthesize fraction of the tick
     * from wall clock so animations never step at pure 20Hz.
     */
    public static float resolvePartialTick(Minecraft mc) {
        float pt = 0f;
        try {
            pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        } catch (Throwable ignored) {}
        if (pt <= 1e-5f) {
            try {
                pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            } catch (Throwable ignored) {}
        }
        if (pt > 1e-5f && pt <= 1.0001f) return Math.min(1f, pt);

        long world = 0L;
        if (mc.level != null) {
            try { world = mc.level.getGameTime(); } catch (Throwable ignored) {}
        }
        long now = System.nanoTime();
        if (world != smoothWorldAnchor) {
            smoothWorldAnchor = world;
            smoothEpochNs = now;
        }
        double frac = (now - smoothEpochNs) / 50_000_000.0;
        if (frac < 0) frac = 0;
        if (frac > 0.999) frac = 0.999;
        return (float) frac;
    }

    public static double smoothSeconds(Minecraft mc) {
        long gt = 0L;
        if (mc.level != null) {
            try { gt = mc.level.getGameTime(); } catch (Throwable ignored) {}
        }
        return (gt + resolvePartialTick(mc)) / 20.0;
    }

    private static void extractFresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            frame = new Frame(List.of());
            return;
        }
        float pt = resolvePartialTick(mc);

        List<Shape> out = new ArrayList<>(64);
        Sink sink = new Sink() {
            @Override
            public void line(Vec3 a, Vec3 b, float w, boolean ss, int argb, boolean xray) {
                if (out.size() > 12_000) return;
                out.add(new Seg(a, b, w, ss, argb, xray));
            }

            @Override
            public void box(double x0, double y0, double z0, double x1, double y1, double z1, int argb, boolean xray) {
                if (out.size() > 12_000) return;
                out.add(new Fill(x0, y0, z0, x1, y1, z1, argb, xray));
            }

            @Override
            public void tracer(Vec3 t, float w, int argb) {
                if (out.size() > 12_000) return;
                out.add(new Tr(t, w, argb));
            }
        };
        for (Extractor e : EXTRACTORS) {
            try {
                e.extract(sink, mc, pt);
            } catch (Throwable t) {
                long now = System.nanoTime();
                if (now - lastFailLogNs > 5_000_000_000L) {
                    lastFailLogNs = now;
                    LOG.error("Extractor failed (throttled)", t);
                }
            }
        }
        frame = new Frame(List.copyOf(out));
    }

    private static Vec3 cameraForward(Minecraft mc, float pt) {
        float yaw;
        float pitch;
        if (FreecamController.isActive()) {
            yaw = FreecamController.yaw();
            pitch = FreecamController.pitch();
        } else if (FreelookController.isActive()) {
            yaw = FreelookController.yaw();
            pitch = FreelookController.pitch();
        } else if (mc.player != null) {
            yaw = mc.player.getViewYRot(pt);
            pitch = mc.player.getViewXRot(pt);
        } else {
            yaw = 0;
            pitch = 0;
        }
        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);
        return new Vec3(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
    }

    private static void draw(LevelRenderContext ctx) {
        Frame f = frame;
        if (f.shapes().isEmpty()) return;
        if (failStreak >= MAX_FAIL_STREAK) {
            if ((System.nanoTime() / 1_000_000_000L) % 5 == 0) failStreak = MAX_FAIL_STREAK / 2;
            return;
        }
        try {
            drawFrame(ctx, f);
            failStreak = 0;
        } catch (Throwable t) {
            failStreak++;
            long now = System.nanoTime();
            if (now - lastFailLogNs > 3_000_000_000L) {
                lastFailLogNs = now;
                LOG.error("VoidDraw frame failed (streak={}) — will retry", failStreak, t);
            }
            frame = new Frame(List.of());
        }
    }

    private static void drawFrame(LevelRenderContext ctx, Frame f) {
        Minecraft mc = Minecraft.getInstance();
        VertexFormat fmt = XRAY.getVertexFormatBinding(0);
        if (fmt == null) {
            long now = System.nanoTime();
            if (now - lastFailLogNs > 10_000_000_000L) {
                lastFailLogNs = now;
                LOG.error("VoidDraw: no vertex format on DEBUG_FILLED pipeline");
            }
            return;
        }
        PrimitiveTopology topo = XRAY.getPrimitiveTopology();
        var sorting = topo == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null;

        StagedVertexBuffer.Draw dX = BUF.appendDraw(fmt, topo, sorting);
        StagedVertexBuffer.Draw dD = BUF.appendDraw(fmt, topo, sorting);
        VertexConsumer vX = BUF.getVertexBuilder(dX);
        VertexConsumer vD = BUF.getVertexBuilder(dD);

        PoseStack pose = ctx.poseStack();
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4fc m = pose.last().pose();

        float pt = resolvePartialTick(mc);
        Vec3 fwd = cameraForward(mc, pt);
        Vec3 origin = new Vec3(cam.x + fwd.x * 0.45, cam.y + fwd.y * 0.45, cam.z + fwd.z * 0.45);

        for (Shape s : f.shapes()) {
            if (s instanceof Seg g) {
                ribbon(m, g.xray() ? vX : vD, g.a(), g.b(), cam, g.width(), g.screenScale(), g.argb());
            } else if (s instanceof Tr t) {
                ribbon(m, vX, origin, t.target(), cam, t.width(), true, t.argb());
            } else if (s instanceof Fill b) {
                box(m, b.xray() ? vX : vD, b);
            }
        }
        pose.popPose();

        BUF.upload();
        StagedVertexBuffer.ExecuteInfo iX = BUF.getExecuteInfo(dX);
        StagedVertexBuffer.ExecuteInfo iD = BUF.getExecuteInfo(dD);
        if (iX != null) execute(mc, iX, XRAY);
        if (iD != null) execute(mc, iD, DEPTH);
        BUF.endFrame();
    }

    private static void ribbon(Matrix4fc m, VertexConsumer vb, Vec3 a, Vec3 b, Vec3 cam,
                               float width, boolean screenScale, int argb) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0e-4) return;
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 toCam = cam.subtract(mid);
        Vec3 side = d.cross(toCam);
        double sl = side.length();
        if (sl < 1.0e-4 * len) return;
        double w = screenScale ? width * 0.002 * Math.max(1.0, toCam.length()) : width;
        side = side.scale(w * 0.5 / sl);

        float r = ((argb >> 16) & 255) / 255f;
        float g = ((argb >> 8) & 255) / 255f;
        float bl = (argb & 255) / 255f;
        float al = ((argb >>> 24) & 255) / 255f;
        Vec3 p0 = a.add(side), p1 = a.subtract(side), p2 = b.subtract(side), p3 = b.add(side);
        quad(m, vb, p0, p1, p2, p3, r, g, bl, al);
        quad(m, vb, p3, p2, p1, p0, r, g, bl, al);
    }

    private static void quad(Matrix4fc m, VertexConsumer vb, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float r, float g, float bl, float al) {
        vb.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, al);
        vb.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, al);
        vb.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, bl, al);
        vb.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, al);
    }

    private static void box(Matrix4fc m, VertexConsumer vb, Fill b) {
        float r = ((b.argb() >> 16) & 255) / 255f;
        float g = ((b.argb() >> 8) & 255) / 255f;
        float bl = (b.argb() & 255) / 255f;
        float al = ((b.argb() >>> 24) & 255) / 255f;
        Vec3 a000 = new Vec3(b.x0(), b.y0(), b.z0()), a100 = new Vec3(b.x1(), b.y0(), b.z0());
        Vec3 a110 = new Vec3(b.x1(), b.y1(), b.z0()), a010 = new Vec3(b.x0(), b.y1(), b.z0());
        Vec3 a001 = new Vec3(b.x0(), b.y0(), b.z1()), a101 = new Vec3(b.x1(), b.y0(), b.z1());
        Vec3 a111 = new Vec3(b.x1(), b.y1(), b.z1()), a011 = new Vec3(b.x0(), b.y1(), b.z1());
        Vec3[][] faces = {
                {a001, a101, a111, a011}, {a100, a000, a010, a110},
                {a000, a001, a011, a010}, {a101, a100, a110, a111},
                {a011, a111, a110, a010}, {a000, a100, a101, a001}
        };
        for (Vec3[] q : faces) {
            quad(m, vb, q[0], q[1], q[2], q[3], r, g, bl, al);
            quad(m, vb, q[3], q[2], q[1], q[0], r, g, bl, al);
        }
    }

    private static void execute(Minecraft mc, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dyn = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        if (color == null) return;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "voidclient shapes", color, Optional.empty(),
                        target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dyn);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }
}
