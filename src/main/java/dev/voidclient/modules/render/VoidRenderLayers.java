package dev.voidclient.modules.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

/** A line layer explicitly configured to render ESP edges through opaque geometry. */
public final class VoidRenderLayers {
    private static final RenderPipeline ESP_LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("voidclient", "pipeline/esp_lines"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    public static final RenderLayer ESP_LINES = RenderLayer.of(
            "voidclient_esp_lines",
            RenderSetup.builder(ESP_LINES_PIPELINE).expectedBufferSize(4096).build()
    );

    /** Load and register the pipeline early, before the first world render. */
    public static void bootstrap() {}

    private VoidRenderLayers() {}
}
