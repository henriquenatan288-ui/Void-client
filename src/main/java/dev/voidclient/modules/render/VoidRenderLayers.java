package dev.voidclient.modules.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** Line layer configured to draw ESP edges through opaque geometry (no depth). */
public final class VoidRenderLayers {
    public static final RenderPipeline ESP_LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("voidclient", "pipeline/esp_lines"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    public static final RenderType ESP_LINES = RenderType.create(
            "voidclient_esp_lines",
            RenderSetup.builder(ESP_LINES_PIPELINE).createRenderSetup()
    );

    /** Load and register the pipeline early, before the first world render. */
    public static void bootstrap() {}

    private VoidRenderLayers() {}
}
