package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/** Cosmetic particle patterns around the local player.
 *  Phase is driven by world time for continuous smooth rotation. */
public class Particles extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting(
            "Style", "Particle style", "Crown", "Crown", "Sword", "Totem", "Mix"));
    private boolean loggedFirstSpawn;

    public Particles() {
        super("Particles", "Show a refreshed particle pattern around your player.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        loggedFirstSpawn = false;
        VoidClient.LOGGER.info("Particles enabled; style={}", mode.get());
    }

    @Override
    protected void onDisable() {
        loggedFirstSpawn = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        // Use world time so the phase advances smoothly even under low TPS
        long worldTime = mc.world.getTime();
        if (worldTime % 2 != 0) return;

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float phaseBase = worldTime * 0.11f;

        switch (mode.get()) {
            case "Crown" -> crown(mc, x, y, z, phaseBase);
            case "Sword" -> sword(mc, x, y, z, phaseBase);
            case "Totem" -> totem(mc, x, y, z, phaseBase);
            case "Mix" -> {
                switch ((int) ((worldTime / 2) % 3)) {
                    case 0 -> crown(mc, x, y, z, phaseBase);
                    case 1 -> sword(mc, x, y, z, phaseBase);
                    default -> totem(mc, x, y, z, phaseBase);
                }
            }
            default -> crown(mc, x, y, z, phaseBase);
        }
    }

    private void crown(MinecraftClient mc, double x, double y, double z, float phase) {
        for (int i = 0; i < 8; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 8.0);
            emit(mc, ParticleTypes.END_ROD,
                    x + Math.cos(angle) * 0.62,
                    y + 1.48 + Math.sin(angle * 2.0) * 0.12,
                    z + Math.sin(angle) * 0.62,
                    0, 0.015, 0);
        }
    }

    private void sword(MinecraftClient mc, double x, double y, double z, float phase) {
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

    private void totem(MinecraftClient mc, double x, double y, double z, float phase) {
        for (int i = 0; i < 8; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 8.0);
            emit(mc, ParticleTypes.TOTEM_OF_UNDYING,
                    x + Math.cos(angle) * 0.65,
                    y + 0.45 + (i % 3) * 0.35,
                    z + Math.sin(angle) * 0.65,
                    0, 0.12, 0);
        }
    }

    private void emit(MinecraftClient mc, ParticleEffect effect,
                      double x, double y, double z,
                      double velocityX, double velocityY, double velocityZ) {
        mc.world.addParticleClient(effect, true, true,
                x, y, z, velocityX, velocityY, velocityZ);
        if (!loggedFirstSpawn) {
            loggedFirstSpawn = true;
            VoidClient.LOGGER.info("Particles emitted first effect={} at {},{},{}",
                    effect.getType(), x, y, z);
        }
    }
}
