package dev.voidclient.modules.combat;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/** Visual heavy-hit effect plus a slower local hand-swing animation. */
public class SlowHit extends Module {
    private final BooleanSetting critFx = addSetting(
            new BooleanSetting("Crit FX", "Extra crit particles on attack", true));
    private final NumberSetting swingDuration = addSetting(
            new NumberSetting("Swing duration", "Ticks per visible arm swing", 14, 6, 30, 1));

    private boolean wasAttacking;

    public SlowHit() {
        super("SlowHit", "Slow the visible attack/mining arm animation.", Category.COMBAT);
    }

    public int getVisualSwingDuration() {
        return swingDuration.getInt();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        boolean attacking = mc.options.attackKey.isPressed();
        if (attacking && !wasAttacking) {
            HitResult hit = mc.crosshairTarget;
            if (hit instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();
                double x = target.getX();
                double y = target.getBodyY(0.5);
                double z = target.getZ();

                for (int i = 0; i < 12; i++) {
                    mc.world.addParticleClient(ParticleTypes.CRIT, x, y, z,
                            (Math.random() - 0.5) * 0.5,
                            Math.random() * 0.4,
                            (Math.random() - 0.5) * 0.5);
                }
                if (critFx.get()) {
                    for (int i = 0; i < 8; i++) {
                        mc.world.addParticleClient(ParticleTypes.ENCHANTED_HIT, x, y, z,
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
