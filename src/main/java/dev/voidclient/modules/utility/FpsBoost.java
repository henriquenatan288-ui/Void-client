package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;

/**
 * Client-side FPS helpers for mobile: cut particles, clouds, weather, entity distance.
 * Does not kill OS processes — only Minecraft client work.
 */
public class FpsBoost extends Module {
    private final BooleanSetting noParticles = addSetting(
            new BooleanSetting("Minimal particles", "Force minimal particle setting", true));
    private final BooleanSetting noClouds = addSetting(
            new BooleanSetting("No clouds", "Disable cloud rendering", true));
    private final BooleanSetting noWeather = addSetting(
            new BooleanSetting("No weather", "Hide rain/snow", true));
    private final BooleanSetting noVignette = addSetting(
            new BooleanSetting("No vignette", "Disable vignette overlay", true));
    private final NumberSetting entityDist = addSetting(
            new NumberSetting("Entity distance %", "Entity render distance scale", 50, 25, 100, 5));

    private ParticleStatus prevParticles;
    private double prevEntityScale = 1.0;
    private boolean applied;

    public FpsBoost() {
        super("FpsBoost", "Reduce client render load (mobile friendly).", Category.UTILITY);
    }

    public boolean hideWeather() { return isEnabled() && noWeather.get(); }
    public boolean hideVignette() { return isEnabled() && noVignette.get(); }
    public boolean hideClouds() { return isEnabled() && noClouds.get(); }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        restore();
    }

    @Override
    public void onTick() {
        if (!applied) apply();
    }

    private void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        try {
            if (noParticles.get()) {
                prevParticles = mc.options.particles().get();
                mc.options.particles().set(ParticleStatus.MINIMAL);
            }
        } catch (Throwable ignored) {}
        try {
            prevEntityScale = mc.options.entityDistanceScaling().get();
            mc.options.entityDistanceScaling().set(entityDist.get() / 100.0);
        } catch (Throwable ignored) {}
        applied = true;
    }

    private void restore() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        try {
            if (prevParticles != null) mc.options.particles().set(prevParticles);
        } catch (Throwable ignored) {}
        try {
            mc.options.entityDistanceScaling().set(prevEntityScale);
        } catch (Throwable ignored) {}
        prevParticles = null;
        applied = false;
    }
}
