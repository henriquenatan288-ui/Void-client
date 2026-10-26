package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.mixin.SimpleOptionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

public class Fullbright extends Module {
    private Double previousGamma;

    public Fullbright() {
        super("Fullbright", "Maximum brightness, restores the previous gamma when disabled.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        SimpleOption<Double> gamma = mc.options.getGamma();
        previousGamma = gamma.getValue();
        ((SimpleOptionAccessor<Double>) (Object) gamma).voidclient$setValueDirect(16.0);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null || previousGamma == null) return;
        ((SimpleOptionAccessor<Double>) (Object) mc.options.getGamma()).voidclient$setValueDirect(previousGamma);
        previousGamma = null;
    }
}
