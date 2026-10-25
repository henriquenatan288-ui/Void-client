package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;

import java.util.Comparator;
import java.util.List;

/** Draws visible dotted particle trails from the local player to nearby players. */
public class Tracers extends Module {
    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Maximum target distance", 64, 8, 128, 4));
    private final NumberSetting dots = addSetting(
            new NumberSetting("Dots", "Dots in each tracer", 16, 4, 28, 2));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Tracer color", 0xFF00FFAA));
    private int ticks;

    public Tracers() {
        super("Tracers", "Show dotted trails to other players.", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (++ticks % 2 != 0) return;

        double maxDistance = range.get();
        double maxDistanceSq = maxDistance * maxDistance;
        double startX = mc.player.getX();
        double startY = mc.player.getEyeY();
        double startZ = mc.player.getZ();
        DustParticleEffect dust = new DustParticleEffect(color.getRgb(), 1.35f);

        List<PlayerEntity> targets = mc.world.getPlayers().stream()
                .filter(other -> other != mc.player)
                .filter(other -> mc.player.squaredDistanceTo(other) <= maxDistanceSq)
                .sorted(Comparator.comparingDouble(other -> mc.player.squaredDistanceTo(other)))
                .limit(10)
                .toList();

        int count = dots.getInt();
        for (PlayerEntity target : targets) {
            double endX = target.getX();
            double endY = target.getBodyY(0.55);
            double endZ = target.getZ();
            for (int i = 1; i <= count; i++) {
                double t = i / (double) (count + 1);
                mc.world.addParticleClient(dust,
                        startX + (endX - startX) * t,
                        startY + (endY - startY) * t,
                        startZ + (endZ - startZ) * t,
                        0, 0, 0);
            }
        }
    }
}
