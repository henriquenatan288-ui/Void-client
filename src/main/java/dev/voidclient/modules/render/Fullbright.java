package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.mixin.SimpleOptionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/** Fullbright by directly changing the gamma option value, restoring it on disable. */
public class Fullbright extends Module {
    private Double previousGamma;

    public Fullbright() {
        super("Fullbright", "Maximum brightness, restores the previous gamma when disabled.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;

        OptionInstance<Double> gamma = mc.options.gamma();
        previousGamma = gamma.get();
        ((SimpleOptionAccessor) (Object) gamma).voidclient$setValueDirect(16.0);
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || previousGamma == null) return;

        ((SimpleOptionAccessor) (Object) mc.options.gamma()).voidclient$setValueDirect(previousGamma);
        previousGamma = null;
    }
}
