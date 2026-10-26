package dev.voidclient.modules.misc;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Simple FOV zoom while enabled. */
public class Zoom extends Module {
    private final NumberSetting fov = addSetting(
            new NumberSetting("FOV", "Zoom field of view", 30, 10, 70, 1));
    private double previousFov = -1;

    public Zoom() {
        super("Zoom", "Zoom in by lowering FOV.", Category.MISC);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        previousFov = mc.options.fov().get();
        mc.options.fov().set((int) fov.get());
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || previousFov < 0) return;
        mc.options.fov().set((int) previousFov);
        previousFov = -1;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        int target = (int) fov.get();
        if (mc.options.fov().get() != target) {
            mc.options.fov().set(target);
        }
    }
}
