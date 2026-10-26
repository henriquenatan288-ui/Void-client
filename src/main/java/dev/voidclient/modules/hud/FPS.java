package dev.voidclient.modules.hud;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Simple FPS counter on the HUD.
 *
 * Confiança: ALTA
 * - MinecraftClient.getCurrentFps() existe em 1.21.x.
 * - DrawContext.drawText é a API correta de renderização 2D.
 */
public class FPS extends Module {

    private final NumberSetting x = addSetting(new NumberSetting("X", "Horizontal position", 4, 0, 2000, 1));
    private final NumberSetting y = addSetting(new NumberSetting("Y", "Vertical position", 4, 0, 2000, 1));
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Text color", 0xFFAA00FF)); // neon purple
    private final BooleanSetting shadow = addSetting(new BooleanSetting("Shadow", "Draw text shadow", true));

    public FPS() {
        super("FPS", "Shows current frames per second.", Category.HUD);
    }

    /**
     * Called from ModuleManager when Render2DEvent is fired.
     */
    public void render(GuiGraphicsExtractor context, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || context == null) return;

        String text = mc.getFps() + " FPS";
        context.text(mc.font, text, x.getInt(), y.getInt(), color.get(), shadow.get());
    }
}
