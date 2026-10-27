package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/**
 * Aggressive client-side FPS boost by disabling / reducing expensive visual features.
 * All original settings are restored when the module is turned off.
 */
public class FpsBoost extends Module {
    private final BooleanSetting noParticles = addSetting(
            new BooleanSetting("No particles", "Force particles to Minimal", true));
    private final BooleanSetting noClouds = addSetting(
            new BooleanSetting("No clouds", "Disable cloud rendering", true));
    private final BooleanSetting noVsync = addSetting(
            new BooleanSetting("No VSync", "Disable vertical sync", true));
    private final BooleanSetting fastGraphics = addSetting(
            new BooleanSetting("Fast graphics", "Force Fast graphics mode", true));
    private final NumberSetting entityDist = addSetting(
            new NumberSetting("Entity distance %", "Entity render distance scale (0-100)", 50, 10, 100, 5));

    // Saved values for restore
    private ParticlesMode savedParticles;
    private CloudRenderMode savedClouds;
    private boolean savedVsync;
    private GraphicsMode savedGraphics;
    private double savedEntityDist;
    private boolean saved;

    public FpsBoost() {
        super("FpsBoost", "Hide/disable expensive Minecraft visuals for higher FPS.", Category.UTILITY);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        // Save current values
        savedParticles = mc.options.getParticles().getValue();
        savedClouds = mc.options.getCloudRenderMode().getValue();
        savedVsync = mc.options.getEnableVsync().getValue();
        savedGraphics = mc.options.getGraphicsMode().getValue();
        savedEntityDist = mc.options.getEntityDistanceScaling().getValue();
        saved = true;

        apply();
    }

    @Override
    public void onTick() {
        // Re-apply every few ticks in case another mod or the options screen changes them
        if (isEnabled() && saved) apply();
    }

    private void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        if (noParticles.get()) {
            mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
        }
        if (noClouds.get()) {
            mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        }
        if (noVsync.get()) {
            mc.options.getEnableVsync().setValue(false);
        }
        if (fastGraphics.get()) {
            mc.options.getGraphicsMode().setValue(GraphicsMode.FAST);
        }
        double scale = entityDist.get() / 100.0;
        mc.options.getEntityDistanceScaling().setValue(scale);
    }

    @Override
    protected void onDisable() {
        if (!saved) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        if (savedParticles != null) mc.options.getParticles().setValue(savedParticles);
        if (savedClouds != null) mc.options.getCloudRenderMode().setValue(savedClouds);
        mc.options.getEnableVsync().setValue(savedVsync);
        if (savedGraphics != null) mc.options.getGraphicsMode().setValue(savedGraphics);
        mc.options.getEntityDistanceScaling().setValue(savedEntityDist);

        saved = false;
        savedParticles = null;
        savedClouds = null;
        savedGraphics = null;
    }
}
