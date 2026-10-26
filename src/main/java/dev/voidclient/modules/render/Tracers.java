package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import java.util.Comparator;
import java.util.List;

/** Player tracers are rendered as 3D lines by VoidWorldRenderer; no particles are spawned. */
public class Tracers extends Module {
    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Maximum target distance", 64, 8, 128, 4));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Tracer color", 0xFF00FFAA));
    private List<AbstractClientPlayerEntity> targets = List.of();

    public Tracers() {
        super("Tracers", "Draw 3D lines from the camera to nearby players.", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            targets = List.of();
            return;
        }
        double maxSquared = range.get() * range.get();
        targets = mc.world.getPlayers().stream()
                .filter(other -> other != mc.player)
                .filter(other -> mc.player.squaredDistanceTo(other) <= maxSquared)
                .sorted(Comparator.comparingDouble(other -> mc.player.squaredDistanceTo(other)))
                .limit(32)
                .toList();
    }

    @Override
    protected void onDisable() { targets = List.of(); }

    public List<AbstractClientPlayerEntity> getTargets() { return targets; }
    public int getTracerColor() { return color.get(); }
}
