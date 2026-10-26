package dev.voidclient.modules.misc;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Simple FOV zoom while enabled. */
public class Zoom extends Module {
    private final NumberSetting fov = addSetting(
            new NumberSetting("FOV", "Zoom field of view", 30, 10, 70, 1));
    private Integer previousFov = null;

    public Zoom() {
        super("Zoom", "Zoom in by lowering FOV.", Category.MISC);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        previousFov = mc.options.fov().get();
        mc.options.fov().set(Integer.valueOf(fov.getInt()));
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || previousFov == null) return;
        mc.options.fov().set(previousFov);
        previousFov = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        Integer target = Integer.valueOf(fov.getInt());
        if (!target.equals(mc.options.fov().get())) {
            mc.options.fov().set(target);
        }
    }
}
