package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

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

        Vec3d pos = p.getPos().add(0, 1.0, 0);
        String m = mode.get();

        switch (m) {
            case "Crown" -> crown(mc, pos);
            case "Sword" -> sword(mc, pos);
            case "Totem" -> totem(mc, pos);
            case "Mix" -> {
                if (tick % 12 < 4) crown(mc, pos);
                else if (tick % 12 < 8) sword(mc, pos);
                else totem(mc, pos);
            }
            default -> crown(mc, pos);
        }
    }

    private void crown(MinecraftClient mc, Vec3d pos) {
        double t = tick * 0.25;
        for (int i = 0; i < 6; i++) {
            double a = t + i * (Math.PI * 2 / 6);
            mc.world.addParticleClient(ParticleTypes.END_ROD,
                    pos.x + Math.cos(a) * 0.6,
                    pos.y + 1.2,
                    pos.z + Math.sin(a) * 0.6,
                    0, 0.02, 0);
        }
    }

    private void sword(MinecraftClient mc, Vec3d pos) {
        mc.world.addParticleClient(ParticleTypes.CRIT,
                pos.x, pos.y + 0.5, pos.z, 0, 0.1, 0);
        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT,
                pos.x, pos.y + 0.8, pos.z, 0, 0.05, 0);
    }

    private void totem(MinecraftClient mc, Vec3d pos) {
        mc.world.addParticleClient(ParticleTypes.TOTEM_OF_UNDYING,
                pos.x, pos.y + 0.5, pos.z,
                (Math.random() - 0.5) * 0.3,
                0.2,
                (Math.random() - 0.5) * 0.3);
    }
}
