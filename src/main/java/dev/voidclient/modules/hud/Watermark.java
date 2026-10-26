package dev.voidclient.modules.hud;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Void Client watermark on screen.
 */
public class Watermark extends Module {

    private final NumberSetting x = addSetting(
            new NumberSetting("X", "Horizontal", 4, 0, 2000, 1));
    private final NumberSetting y = addSetting(
            new NumberSetting("Y", "Vertical", 16, 0, 2000, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Text color", 0xFFFFFFFF));

    public Watermark() {
        super("Watermark", "Void Client brand on HUD.", Category.HUD);
        setEnabled(true);
    }

    public void render(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        String text = "Void Client " + VoidClient.VERSION + " §7| §fbisteck";
        ctx.drawString(mc.font, text, x.getInt(), y.getInt(), color.get(), true);
    }
}
