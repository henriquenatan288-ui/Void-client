package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;

/** Cosmetic particle patterns around the local player. */
public class Particles extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting(
            "Style", "Particle style", "Crown", "Crown", "Sword", "Totem", "Mix"));
    private int tick;

    public Particles() {
        super("Particles", "Show a refreshed particle pattern around your player.", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;
        if (++tick % 2 != 0) return;

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        switch (mode.get()) {
            case "Crown" -> crown(mc, x, y, z);
            case "Sword" -> sword(mc, x, y, z);
            case "Totem" -> totem(mc, x, y, z);
            case "Mix" -> {
                switch ((tick / 2) % 3) {
                    case 0 -> crown(mc, x, y, z);
                    case 1 -> sword(mc, x, y, z);
                    default -> totem(mc, x, y, z);
                }
            }
            default -> crown(mc, x, y, z);
        }
    }

    private void crown(MinecraftClient mc, double x, double y, double z) {
        double phase = tick * 0.22;
        // Keep some particles near shoulder height so they can be seen in first person too.
        for (int i = 0; i < 8; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 8.0);
            mc.world.addParticleClient(ParticleTypes.END_ROD,
                    x + Math.cos(angle) * 0.48,
                    y + 1.25 + Math.sin(angle * 2.0) * 0.12,
                    z + Math.sin(angle) * 0.48,
                    0, 0.015, 0);
        }
    }

    private void sword(MinecraftClient mc, double x, double y, double z) {
        double phase = tick * 0.18;
        for (int i = 0; i < 6; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 6.0);
            mc.world.addParticleClient(ParticleTypes.CRIT,
                    x + Math.cos(angle) * 0.45,
                    y + 0.95 + i * 0.1,
                    z + Math.sin(angle) * 0.45,
                    0, 0.04, 0);
        }
        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, x, y + 1.15, z, 0, 0.04, 0);
    }

    private void totem(MinecraftClient mc, double x, double y, double z) {
        for (int i = 0; i < 8; i++) {
            double angle = tick * 0.16 + i * (Math.PI * 2.0 / 8.0);
            mc.world.addParticleClient(ParticleTypes.TOTEM_OF_UNDYING,
                    x + Math.cos(angle) * 0.5,
                    y + 0.45 + (i % 3) * 0.35,
                    z + Math.sin(angle) * 0.5,
                    0, 0.12, 0);
        }
    }
}
