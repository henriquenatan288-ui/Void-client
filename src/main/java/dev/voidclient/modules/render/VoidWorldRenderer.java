package dev.voidclient.modules.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

/**
 * Minecraft 26.2 moved world rendering to the submit-node pipeline.
 * The legacy BufferSource/RenderType drawing path is intentionally disabled
 * until the ESP geometry is migrated to a registered FeatureRenderer.
 */
public final class VoidWorldRenderer {
    private VoidWorldRenderer() {}

    public static void render(LevelRenderContext context) {
        // TODO: submit ESP boxes and tracers through the 26.2 feature-renderer API.
    }
}
