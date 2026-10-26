package dev.voidclient.core.event.events;

import dev.voidclient.core.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Fired every frame for HUD / 2D rendering.
 * DrawContext is the Yarn name; if the project uses Mojang mappings it may be GuiGraphicsExtractor.
 */
public class Render2DEvent extends Event {

    private final GuiGraphicsExtractor context;
    private final float tickDelta;

    public Render2DEvent(GuiGraphicsExtractor context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }

    public GuiGraphicsExtractor getContext() {
        return context;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
