package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Expanding wave rings when a block is broken.
 */
public class BreakWaves extends Module {

    private final NumberSetting waves = addSetting(
            new NumberSetting("Waves", "Number of waves", 3, 1, 6, 1));

    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Wave color", 0xFFFFFFFF));

    private final List<Wave> active = new ArrayList<>();
    private static boolean registered;

    public BreakWaves() {
        super("BreakWaves", "Waves when you break a block.", Category.RENDER);
        if (!registered) {
            registered = true;
            PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || player != mc.player) return;
                if (dev.voidclient.VoidClient.MODULE_MANAGER == null) return;
                Module m = dev.voidclient.VoidClient.MODULE_MANAGER.getByName("BreakWaves");
                if (m instanceof BreakWaves bw && bw.isEnabled()) {
                    bw.spawn(pos);
                }
            });
        }
    }

    private void spawn(BlockPos pos) {
        active.add(new Wave(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            active.clear();
            return;
        }

        int rgb = color.get() & 0x00FFFFFF;
        DustParticleEffect dust = new DustParticleEffect(rgb, 0.8f);

        int maxWaves = waves.getInt();
        Iterator<Wave> it = active.iterator();
        while (it.hasNext()) {
            Wave w = it.next();
            w.age++;

            for (int ring = 0; ring < maxWaves; ring++) {
                int localAge = w.age - ring * 3;
                if (localAge < 0 || localAge > 12) continue;
                double radius = localAge * 0.35;
                int points = 16;
                for (int i = 0; i < points; i++) {
                    double a = (Math.PI * 2 * i) / points;
                    mc.world.addParticleClient(dust,
                            w.x + Math.cos(a) * radius,
                            w.y,
                            w.z + Math.sin(a) * radius,
                            0, 0.01, 0);
                }
            }

            if (w.age > 12 + maxWaves * 3) it.remove();
        }
    }

    private static class Wave {
        final double x, y, z;
        int age;

        Wave(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
