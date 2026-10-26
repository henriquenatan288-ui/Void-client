package dev.voidclient.modules.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

/**
 * Minecraft 26.2 removed the legacy MultiBufferSource / renderBuffers path.
 *
 * BlockESP / PlayerESP / Tracers still:
 *  - scan targets every tick
 *  - draw HUD text (names, counts)
 *
 * 3D boxes + tracers need the new StagedVertexBuffer + custom RenderPipeline
 * path (see Fabric docs "Rendering in the World"). That requires genSources
 * to confirm exact Camera / LevelRenderContext field names before wiring.
 */
public final class VoidWorldRenderer {
    private VoidWorldRenderer() {}

    public static void render(LevelRenderContext context) {
        // Intentionally empty until StagedVertexBuffer ESP is implemented.
    }
}
