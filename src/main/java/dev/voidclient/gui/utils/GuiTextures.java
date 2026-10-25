package dev.voidclient.gui.utils;

import net.minecraft.util.Identifier;

/**
 * Texture identifiers for the ClickGUI.
 * Assets: assets/voidclient/textures/gui/
 */
public final class GuiTextures {

    private GuiTextures() {}

    public static final Identifier PANEL  = id("textures/gui/panel.png");
    public static final Identifier CARD   = id("textures/gui/card.png");
    public static final Identifier SHADOW = id("textures/gui/shadow.png");
    public static final Identifier WHITE  = id("textures/gui/white.png");
    public static final Identifier ACCENT = id("textures/gui/accent.png");

    private static Identifier id(String path) {
        return Identifier.of("voidclient", path);
    }
}
