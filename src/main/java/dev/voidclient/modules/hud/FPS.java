package dev.voidclient.modules.hud;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.modules.personalization.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Simple FPS counter on the HUD. */
public class FPS extends Module {

    private final NumberSetting x = addSetting(new NumberSetting("X", "Horizontal position", 4, 0, 2000, 1));
    private final NumberSetting y = addSetting(new NumberSetting("Y", "Vertical position", 4, 0, 2000, 1));
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Text color", 0xFFAA00FF));
    private final BooleanSetting shadow = addSetting(new BooleanSetting("Shadow", "Draw text shadow", true));

    public FPS() {
        super("FPS", "Shows current frames per second.", Category.HUD);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor context, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || context == null) return;

        String text = mc.getFps() + " FPS";
        int drawColor = HudTheme.resolve(color.get());
        context.text(mc.font, text, x.getInt(), y.getInt(), drawColor, shadow.get());
    }
}
