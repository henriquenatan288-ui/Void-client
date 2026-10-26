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
 * Custom orbiting symbols around the local player.
 * Distance and quantity are configurable.
 */
public class Particles extends Module {
    private final ModeSetting style = addSetting(new ModeSetting(
            "Style", "Symbol style", "Rod", "Rod", "Crit", "Enchant", "Mix"));
    private final NumberSetting distance = addSetting(
            new NumberSetting("Distance", "Orbit radius around the player", 0.85, 0.4, 2.5, 0.05));
    private final NumberSetting amount = addSetting(
            new NumberSetting("Amount", "How many symbols orbit the player", 8, 3, 24, 1));
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Orbit spin speed", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting height = addSetting(
            new NumberSetting("Height", "Vertical center of the orbit", 1.0, 0.2, 2.2, 0.05));

    private int tick;

    public Particles() {
        super("Particles", "Orbiting symbols around you (distance + amount).", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        tick = 0;
        VoidClient.LOGGER.info("Particles enabled; style={} distance={} amount={}",
                style.get(), distance.get(), amount.getInt());
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        tick++;
        // Smooth orbit every tick
        double x = player.getX();
        double y = player.getY() + height.get();
        double z = player.getZ();
        double radius = distance.get();
        int count = amount.getInt();
        double phase = tick * 0.12 * speed.get();

        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            // Slight vertical wave so it feels alive
            double py = y + Math.sin(angle * 2.0 + phase) * 0.12;

            ParticleOptions effect = pickEffect(i);
            mc.level.addParticle(effect, true, true, px, py, pz, 0, 0.01, 0);
        }
    }

    private ParticleOptions pickEffect(int index) {
        return switch (style.get()) {
            case "Crit" -> ParticleTypes.CRIT;
            case "Enchant" -> ParticleTypes.ENCHANTED_HIT;
            case "Mix" -> switch (index % 3) {
                case 0 -> ParticleTypes.END_ROD;
                case 1 -> ParticleTypes.CRIT;
                default -> ParticleTypes.ENCHANTED_HIT;
            };
            default -> ParticleTypes.END_ROD; // "Rod" – clean white symbol
        };
    }
}
