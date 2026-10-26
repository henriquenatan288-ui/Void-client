package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

/**
 * Orbiting particle styles around the local player.
 * Uses addParticle(..., force, alwaysShow, ...) so Minimal settings still show them when supported.
 */
public class Particles extends Module {
    private final ModeSetting style = addSetting(new ModeSetting(
            "Style", "Particle style", "Crown",
            "Crown", "Sword", "Totem", "Rod", "Crit", "Enchant", "Mix"));
    private final NumberSetting distance = addSetting(
            new NumberSetting("Distance", "Orbit radius", 0.85, 0.4, 2.5, 0.05));
    private final NumberSetting amount = addSetting(
            new NumberSetting("Amount", "Symbol count", 10, 3, 24, 1));
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Spin speed", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting height = addSetting(
            new NumberSetting("Height", "Orbit height", 1.15, 0.2, 2.2, 0.05));

    private int tick;
    private int spawnCount;
    private boolean loggedTick;
    private boolean loggedSpawn;

    public Particles() {
        super("Particles", "Orbiting symbols around you.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        tick = 0;
        spawnCount = 0;
        loggedTick = false;
        loggedSpawn = false;
        VoidClient.LOGGER.info("Particles ON style={} dist={} amount={}",
                style.get(), distance.get(), amount.getInt());
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            VoidClient.LOGGER.debug("Particles tick skipped: player/level null");
            return;
        }
        if (!loggedTick) {
            loggedTick = true;
            VoidClient.LOGGER.info("Particles first onTick ok");
        }

        tick++;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double radius = distance.get();
        int count = amount.getInt();
        double phase = tick * 0.14 * speed.get();
        String s = style.get();

        switch (s) {
            case "Crown" -> crown(mc, x, y, z, radius, count, phase);
            case "Sword" -> sword(mc, x, y, z, radius, count, phase);
            case "Totem" -> totem(mc, x, y, z, radius, count, phase);
            case "Crit" -> orbit(mc, ParticleTypes.CRIT, x, y + height.get(), z, radius, count, phase);
            case "Enchant" -> orbit(mc, ParticleTypes.ENCHANTED_HIT, x, y + height.get(), z, radius, count, phase);
            case "Mix" -> mix(mc, x, y + height.get(), z, radius, count, phase);
            default -> orbit(mc, ParticleTypes.END_ROD, x, y + height.get(), z, radius, count, phase);
        }
    }

    private void crown(Minecraft mc, double x, double y, double z, double radius, int count, double phase) {
        double hy = y + Math.max(height.get(), 1.35);
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            emit(mc, ParticleTypes.END_ROD,
                    x + Math.cos(angle) * radius,
                    hy + Math.sin(angle * 2.0) * 0.1,
                    z + Math.sin(angle) * radius,
                    0, 0.02, 0);
        }
    }

    private void sword(Minecraft mc, double x, double y, double z, double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            double h = y + 0.6 + (i % 5) * 0.18;
            emit(mc, ParticleTypes.CRIT,
                    x + Math.cos(angle) * (radius * 0.7),
                    h,
                    z + Math.sin(angle) * (radius * 0.7),
                    0, 0.05, 0);
        }
        emit(mc, ParticleTypes.ENCHANTED_HIT, x, y + 1.1, z, 0, 0.04, 0);
    }

    private void totem(Minecraft mc, double x, double y, double z, double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            emit(mc, ParticleTypes.TOTEM_OF_UNDYING,
                    x + Math.cos(angle) * radius,
                    y + 0.4 + (i % 4) * 0.28,
                    z + Math.sin(angle) * radius,
                    0, 0.1, 0);
        }
    }

    private void mix(Minecraft mc, double x, double y, double z, double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            ParticleOptions fx = switch (i % 3) {
                case 0 -> ParticleTypes.END_ROD;
                case 1 -> ParticleTypes.CRIT;
                default -> ParticleTypes.ENCHANTED_HIT;
            };
            emit(mc, fx,
                    x + Math.cos(angle) * radius,
                    y + Math.sin(angle * 2.0 + phase) * 0.12,
                    z + Math.sin(angle) * radius,
                    0, 0.01, 0);
        }
    }

    private void orbit(Minecraft mc, ParticleOptions fx, double x, double y, double z,
                       double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            emit(mc, fx,
                    x + Math.cos(angle) * radius,
                    y + Math.sin(angle * 2.0 + phase) * 0.12,
                    z + Math.sin(angle) * radius,
                    0, 0.01, 0);
        }
    }

    private void emit(Minecraft mc, ParticleOptions effect,
                      double x, double y, double z,
                      double vx, double vy, double vz) {
        try {
            // force=true, alwaysShow=true — still visible on low particle settings when API supports it
            mc.level.addParticle(effect, true, true, x, y, z, vx, vy, vz);
            spawnCount++;
            if (!loggedSpawn) {
                loggedSpawn = true;
                VoidClient.LOGGER.info("Particles first spawn type={} at {},{},{}",
                        effect.getType(), x, y, z);
            }
        } catch (Throwable t) {
            VoidClient.LOGGER.error("Particles spawn failed", t);
        }
    }
}
