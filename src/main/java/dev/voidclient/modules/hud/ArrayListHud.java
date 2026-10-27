package dev.voidclient.modules.hud;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.modules.personalization.HudTheme;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ArrayListHud extends Module {
    private final NumberSetting x = addSetting(new NumberSetting("X", "Horizontal", 4, 0, 2000, 1));
    private final NumberSetting y = addSetting(new NumberSetting("Y", "Vertical", 60, 0, 2000, 1));
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Text color", 0xFFFFFFFF));
    private long cachedRevision = Long.MIN_VALUE;
    private Font cachedRenderer;
    private List<Module> cachedEnabled = List.of();

    public ArrayListHud() { super("ArrayList", "Shows active modules.", Category.HUD); }

    private void refreshCache(Font renderer) {
        if (VoidClient.MODULE_MANAGER == null) {
            cachedEnabled = List.of();
        } else {
            List<Module> enabled = new ArrayList<>(VoidClient.MODULE_MANAGER.getEnabled());
            enabled.removeIf(module -> module == this
                    || module.getName().equalsIgnoreCase("ArrayList")
                    || module.getName().equalsIgnoreCase("ClickGUI"));
            enabled.sort(Comparator.comparingInt((Module module) -> -renderer.width(module.getName())));
            cachedEnabled = List.copyOf(enabled);
        }
        cachedRevision = Module.getStateRevision();
        cachedRenderer = renderer;
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null || VoidClient.MODULE_MANAGER == null) return;
        if (cachedRevision != Module.getStateRevision() || cachedRenderer != mc.font) {
            refreshCache(mc.font);
        }
        int drawY = y.getInt();
        int drawX = x.getInt();
        int textColor = HudTheme.resolve(color.get());
        for (Module module : cachedEnabled) {
            ctx.text(mc.font, module.getName(), drawX, drawY, textColor, true);
            drawY += 10;
        }
    }
}
