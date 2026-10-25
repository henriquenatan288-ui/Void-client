package dev.voidclient.modules.hud;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;
import java.util.List;

/**
 * Lists enabled modules on the HUD.
 */
public class ArrayListHud extends Module {

    private final NumberSetting x = addSetting(
            new NumberSetting("X", "Horizontal", 4, 0, 2000, 1));
    private final NumberSetting y = addSetting(
            new NumberSetting("Y", "Vertical", 60, 0, 2000, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Text color", 0xFFFFFFFF));

    public ArrayListHud() {
        super("ArrayList", "Shows active modules.", Category.HUD);
    }

    public void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null || VoidClient.MODULE_MANAGER == null) return;

        List<Module> enabled = VoidClient.MODULE_MANAGER.getEnabled();
        enabled.sort(Comparator.comparingInt(
                (Module m) -> -mc.textRenderer.getWidth(m.getName())));

        int drawY = y.getInt();
        int drawX = x.getInt();
        for (Module m : enabled) {
            if (m.getName().equalsIgnoreCase("ArrayList")) continue;
            if (m.getName().equalsIgnoreCase("ClickGUI")) continue;
            ctx.drawText(mc.textRenderer, m.getName(), drawX, drawY, color.get(), true);
            drawY += 10;
        }
    }
}
