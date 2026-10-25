package dev.voidclient.core.event.events;

import dev.voidclient.core.event.Event;
import net.minecraft.client.gui.DrawContext;

/**
 * Fired every frame for HUD / 2D rendering.
 * DrawContext is the Yarn name; if the project uses Mojang mappings it may be GuiGraphics.
 */
public class Render2DEvent extends Event {

    private final DrawContext context;
    private final float tickDelta;

    public Render2DEvent(DrawContext context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }

    public DrawContext getContext() {
        return context;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
