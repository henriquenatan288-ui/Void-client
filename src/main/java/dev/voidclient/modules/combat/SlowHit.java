package dev.voidclient.modules.combat;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Visual "heavy hit" effect when attacking.
 */
public class SlowHit extends Module {

    private final BooleanSetting critFx = addSetting(
            new BooleanSetting("Crit FX", "Extra crit particles", true));

    private boolean wasAttacking;

    public SlowHit() {
        super("SlowHit", "Heavy hit visual effect.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        boolean attacking = mc.options.attackKey.isPressed();
        if (attacking && !wasAttacking) {
            HitResult hit = mc.crosshairTarget;
            if (hit instanceof EntityHitResult ehr) {
                Entity target = ehr.getEntity();
                double x = target.getX();
                double y = target.getBodyY(0.5);
                double z = target.getZ();

                for (int i = 0; i < 12; i++) {
                    mc.world.addParticleClient(ParticleTypes.CRIT,
                            x, y, z,
                            (Math.random() - 0.5) * 0.5,
                            Math.random() * 0.4,
                            (Math.random() - 0.5) * 0.5);
                }
                if (critFx.get()) {
                    for (int i = 0; i < 8; i++) {
                        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT,
                                x, y, z,
                                (Math.random() - 0.5) * 0.3,
                                Math.random() * 0.3,
                                (Math.random() - 0.5) * 0.3);
                    }
                }
            }
        }
        wasAttacking = attacking;
    }
}
