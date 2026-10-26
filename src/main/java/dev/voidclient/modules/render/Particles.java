package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

/** Cosmetic particle patterns around the local player, including on Minimal particle settings. */
public class Particles extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting(
            "Style", "Particle style", "Crown", "Crown", "Sword", "Totem", "Mix"));
    private int tick;
    private boolean loggedFirstSpawn;

    public Particles() {
        super("Particles", "Show a refreshed particle pattern around your player.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        tick = 0;
        loggedFirstSpawn = false;
        VoidClient.LOGGER.info("Particles enabled; style={}", mode.get());
    }

    @Override
    protected void onDisable() {
        loggedFirstSpawn = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
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

    private void crown(Minecraft mc, double x, double y, double z) {
        double phase = tick * 0.22;
        // Wider than the player's head so some motes remain visible around the first-person view.
        for (int i = 0; i < 8; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 8.0);
            emit(mc, ParticleTypes.END_ROD,
                    x + Math.cos(angle) * 0.62,
                    y + 1.48 + Math.sin(angle * 2.0) * 0.12,
                    z + Math.sin(angle) * 0.62,
                    0, 0.015, 0);
        }
    }

    private void sword(Minecraft mc, double x, double y, double z) {
        double phase = tick * 0.18;
        for (int i = 0; i < 6; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 6.0);
            emit(mc, ParticleTypes.CRIT,
                    x + Math.cos(angle) * 0.55,
                    y + 0.95 + i * 0.1,
                    z + Math.sin(angle) * 0.55,
                    0, 0.04, 0);
        }
        emit(mc, ParticleTypes.ENCHANTED_HIT, x, y + 1.15, z, 0, 0.04, 0);
    }

    private void totem(Minecraft mc, double x, double y, double z) {
        for (int i = 0; i < 8; i++) {
            double angle = tick * 0.16 + i * (Math.PI * 2.0 / 8.0);
            emit(mc, ParticleTypes.TOTEM_OF_UNDYING,
                    x + Math.cos(angle) * 0.65,
                    y + 0.45 + (i % 3) * 0.35,
                    z + Math.sin(angle) * 0.65,
                    0, 0.12, 0);
        }
    }

    private void emit(Minecraft mc, ParticleOptions effect,
                      double x, double y, double z,
                      double velocityX, double velocityY, double velocityZ) {
        // Yarn 1.21.11 supports (effect, force, canSpawnOnMinimal, ...).
        // These cosmetic effects should not disappear solely because particles are set to Minimal.
        mc.level.addParticle(effect, true, true,
                x, y, z, velocityX, velocityY, velocityZ);
        if (!loggedFirstSpawn) {
            loggedFirstSpawn = true;
            VoidClient.LOGGER.info("Particles emitted first effect={} at {},{},{}",
                    effect.getType(), x, y, z);
        }
    }
}
