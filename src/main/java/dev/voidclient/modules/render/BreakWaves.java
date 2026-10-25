package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Expanding rings triggered after the local client successfully breaks a block. */
public class BreakWaves extends Module {
    private final NumberSetting waves = addSetting(
            new NumberSetting("Waves", "Number of expanding rings", 3, 1, 6, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Wave color", 0xFFFFFFFF));

    private final List<Wave> active = new ArrayList<>();

    public BreakWaves() {
        super("BreakWaves", "Show expanding rings when you break a block.", Category.RENDER);
    }

    public static void onClientBlockBreak(BlockPos pos) {
        if (VoidClient.MODULE_MANAGER == null || pos == null) return;
        Module module = VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (module instanceof BreakWaves waves && waves.isEnabled()) {
            waves.spawn(pos);
        }
    }

    private void spawn(BlockPos pos) {
        if (active.size() >= 12) active.remove(0);
        active.add(new Wave(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            active.clear();
            return;
        }

        DustParticleEffect dust = new DustParticleEffect(color.getRgb(), 0.9f);
        int maxWaves = waves.getInt();
        Iterator<Wave> iterator = active.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            wave.age++;

            for (int ring = 0; ring < maxWaves; ring++) {
                int age = wave.age - ring * 3;
                if (age < 0 || age > 12) continue;
                double radius = age * 0.32;
                int points = 18;
                for (int i = 0; i < points; i++) {
                    double angle = Math.PI * 2.0 * i / points;
                    mc.world.addParticleClient(dust,
                            wave.x + Math.cos(angle) * radius,
                            wave.y + 0.03,
                            wave.z + Math.sin(angle) * radius,
                            0, 0.005, 0);
                }
            }
            if (wave.age > 12 + maxWaves * 3) iterator.remove();
        }
    }

    private static final class Wave {
        final double x, y, z;
        int age;
        Wave(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
