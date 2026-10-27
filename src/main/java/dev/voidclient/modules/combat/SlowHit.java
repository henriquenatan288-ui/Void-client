package dev.voidclient.modules.combat;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SlowHit extends Module {
    private final BooleanSetting critFx = addSetting(
            new BooleanSetting("Crit FX", "Extra crit particles on attack", true));
    private final BooleanSetting enchantedLook = addSetting(
            new BooleanSetting("Enchanted look", "Glint on held tool (client visual)", true));
    private final NumberSetting swingDuration = addSetting(
            new NumberSetting("Swing duration", "Ticks per visible arm swing", 14, 6, 30, 1));

    private boolean wasAttacking;

    public SlowHit() {
        super("SlowHit", "Slow the visible attack/mining arm animation.", Category.COMBAT);
    }

    public int getVisualSwingDuration() {
        return swingDuration.getInt();
    }

    public boolean enchantedLook() {
        return enchantedLook.get();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        boolean attacking = mc.options.keyAttack.isDown();
        if (attacking && !wasAttacking) {
            HitResult hit = mc.hitResult;
            if (hit instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();
                double x = target.getX();
                double y = target.getY(0.5);
                double z = target.getZ();

                for (int i = 0; i < 12; i++) {
                    mc.level.addParticle(ParticleTypes.CRIT, x, y, z,
                            (Math.random() - 0.5) * 0.5,
                            Math.random() * 0.4,
                            (Math.random() - 0.5) * 0.5);
                }
                if (critFx.get()) {
                    for (int i = 0; i < 8; i++) {
                        mc.level.addParticle(ParticleTypes.ENCHANTED_HIT, x, y, z,
                                (Math.random() - 0.5) * 0.3,
                                Math.random() * 0.3,
                                (Math.random() - 0.5) * 0.3);
                    }
                }
            }
        }
        wasAttacking = attacking;
    }

    @Override
    protected void onDisable() {
        wasAttacking = false;
    }
}
