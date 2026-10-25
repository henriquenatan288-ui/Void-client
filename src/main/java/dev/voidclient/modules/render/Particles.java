package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;

/**
 * Cosmetic particles: Crown / Sword / Totem / Mix around the player.
 */
public class Particles extends Module {

    private final ModeSetting mode = addSetting(new ModeSetting(
            "Style", "Particle style",
            "Crown",
            "Crown", "Sword", "Totem", "Mix"));

    private int tick;

    public Particles() {
        super("Particles", "Crown / Sword / Totem / Mix particles.", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        tick++;
        if (tick % 3 != 0) return;

        double x = p.getX();
        double y = p.getY() + 1.0;
        double z = p.getZ();
        String m = mode.get();

        switch (m) {
            case "Crown" -> crown(mc, x, y, z);
            case "Sword" -> sword(mc, x, y, z);
            case "Totem" -> totem(mc, x, y, z);
            case "Mix" -> {
                if (tick % 12 < 4) crown(mc, x, y, z);
                else if (tick % 12 < 8) sword(mc, x, y, z);
                else totem(mc, x, y, z);
            }
            default -> crown(mc, x, y, z);
        }
    }

    private void crown(MinecraftClient mc, double x, double y, double z) {
        double t = tick * 0.25;
        for (int i = 0; i < 6; i++) {
            double a = t + i * (Math.PI * 2 / 6);
            mc.world.addParticleClient(ParticleTypes.END_ROD,
                    x + Math.cos(a) * 0.6,
                    y + 1.2,
                    z + Math.sin(a) * 0.6,
                    0, 0.02, 0);
        }
    }

    private void sword(MinecraftClient mc, double x, double y, double z) {
        mc.world.addParticleClient(ParticleTypes.CRIT, x, y + 0.5, z, 0, 0.1, 0);
        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, x, y + 0.8, z, 0, 0.05, 0);
    }

    private void totem(MinecraftClient mc, double x, double y, double z) {
        mc.world.addParticleClient(ParticleTypes.TOTEM_OF_UNDYING,
                x, y + 0.5, z,
                (Math.random() - 0.5) * 0.3,
                0.2,
                (Math.random() - 0.5) * 0.3);
    }
}
