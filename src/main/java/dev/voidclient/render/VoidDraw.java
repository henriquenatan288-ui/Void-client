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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
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
 * World drawing via filled quads (ribbons), not the LINES pipeline.
 * Extract phase collects shapes; draw phase submits after translucent terrain.
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
    private record Frame(List<Shape> shapes, Vec3 forward) {}

    private static volatile Frame frame = new Frame(List.of(), new Vec3(0, 0, 1));
    private static volatile boolean loggedMissingFormat;
    private static volatile boolean loggedDrawFailure;
    private static volatile boolean closed;

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
        LevelExtractionEvents.END_EXTRACTION.register(VoidDraw::extractAll);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(VoidDraw::draw);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> close());
        LOG.info("VoidDraw initialized (ribbon/quad path)");
    }

    private static void extractAll(LevelExtractionContext ctx) {
        if (closed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            frame = new Frame(List.of(), new Vec3(0, 0, 1));
            return;
        }

        float partialTick = Mth.clamp(
                ctx.deltaTracker().getGameTimeDeltaPartialTick(false), 0.0f, 1.0f);

        List<Shape> out = new ArrayList<>();
        Sink sink = new Sink() {
            @Override
            public void line(Vec3 a, Vec3 b, float w, boolean ss, int argb, boolean xray) {
                out.add(new Seg(a, b, w, ss, argb, xray));
            }

            @Override
            public void box(double x0, double y0, double z0, double x1, double y1, double z1, int argb, boolean xray) {
                out.add(new Fill(x0, y0, z0, x1, y1, z1, argb, xray));
            }

            @Override
            public void tracer(Vec3 t, float w, int argb) {
                out.add(new Tr(t, w, argb));
            }
        };
        for (Extractor e : EXTRACTORS) {
            try {
                e.extract(sink, mc, partialTick);
            } catch (Throwable t) {
                LOG.error("Extractor failed", t);
            }
        }
        frame = new Frame(List.copyOf(out), cameraForward(ctx.camera()));
    }

    private static Vec3 cameraForward(Camera camera) {
        // Use the actual interpolated camera direction for this extraction frame.
        // This stays aligned with the renderer camera in normal view, freecam, and freelook.
        var forward = camera.forwardVector();
        return new Vec3(forward.x(), forward.y(), forward.z());
    }

    private static void draw(LevelRenderContext ctx) {
        Frame f = frame;
        if (closed || f.shapes().isEmpty()) return;
        try {
            drawFrame(ctx, f);
            loggedDrawFailure = false;
        } catch (Throwable t) {
            // Do not permanently disable world rendering after a transient GPU/API failure.
            // Log once until a frame renders successfully, avoiding per-frame log spam.
            if (!loggedDrawFailure) {
                LOG.error("VoidDraw render failed; will retry on the next frame", t);
            }
            loggedDrawFailure = true;
        }
    }

    private static void drawFrame(LevelRenderContext ctx, Frame f) {
        Minecraft mc = Minecraft.getInstance();
        VertexFormat fmt = XRAY.getVertexFormatBinding(0);
        if (fmt == null) {
            if (!loggedMissingFormat) {
                LOG.error("VoidDraw: no vertex format on DEBUG_FILLED pipeline");
                loggedMissingFormat = true;
            }
            return;
        }
        loggedMissingFormat = false;
        PrimitiveTopology topo = XRAY.getPrimitiveTopology();
        var sorting = topo == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null;

        boolean frameStarted = false;
        PoseStack pose = ctx.poseStack();
        boolean posePushed = false;
        try {
            StagedVertexBuffer.Draw dX = BUF.appendDraw(fmt, topo, sorting);
            frameStarted = true;
            StagedVertexBuffer.Draw dD = BUF.appendDraw(fmt, topo, sorting);
            VertexConsumer vX = BUF.getVertexBuilder(dX);
            VertexConsumer vD = BUF.getVertexBuilder(dD);

            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            pose.pushPose();
            posePushed = true;
            pose.translate(-cam.x, -cam.y, -cam.z);
            Matrix4fc m = pose.last().pose();

            Vec3 origin = cam.add(f.forward());
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
            posePushed = false;

            BUF.upload();
            StagedVertexBuffer.ExecuteInfo iX = BUF.getExecuteInfo(dX);
            StagedVertexBuffer.ExecuteInfo iD = BUF.getExecuteInfo(dD);
            if (iX != null) execute(mc, iX, XRAY);
            if (iD != null) execute(mc, iD, DEPTH);
        } finally {
            // Restore the extraction/render context even if vertex generation or upload fails.
            try {
                if (posePushed) pose.popPose();
            } finally {
                // StagedVertexBuffer must finish every frame that successfully started,
                // including partially generated frames that fail before upload/submission.
                if (frameStarted) BUF.endFrame();
            }
        }
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

    public static void close() {
        if (closed) return;
        closed = true;
        frame = new Frame(List.of(), new Vec3(0, 0, 1));
        BUF.close();
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
