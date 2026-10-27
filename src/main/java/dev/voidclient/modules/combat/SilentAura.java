package dev.voidclient.modules.combat;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Silent Kill Aura – attacks the nearest valid target without rotating the player body/head.
 * Uses the interaction manager so the server still receives a legitimate attack packet.
 */
public class SilentAura extends Module {
    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Maximum attack distance", 3.5, 2.0, 6.0, 0.1));
    private final NumberSetting cps = addSetting(
            new NumberSetting("CPS", "Attacks per second (approx)", 10, 4, 20, 1));
    private final BooleanSetting playersOnly = addSetting(
            new BooleanSetting("Players only", "Only target other players", true));
    private final BooleanSetting requireWeapon = addSetting(
            new BooleanSetting("Require weapon", "Only attack when holding a sword/axe", false));

    private long lastAttackMs;

    public SilentAura() {
        super("SilentAura", "Silently attack the nearest target without rotating.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null) return;
        if (player.isSpectator() || player.isDead()) return;

        if (requireWeapon.get() && !isWeapon(player.getMainHandStack())) return;

        long now = System.currentTimeMillis();
        long interval = (long) (1000.0 / Math.max(1, cps.getInt()));
        if (now - lastAttackMs < interval) return;

        Entity target = findNearest(player);
        if (target == null) return;

        // Silent: do not change player yaw/pitch. The interaction manager still
        // validates reach from the player's current position.
        mc.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        lastAttackMs = now;
    }

    private Entity findNearest(ClientPlayerEntity player) {
        double maxSq = range.get() * range.get();
        Entity best = null;
        double bestDist = maxSq + 1.0;

        for (Entity entity : player.getWorld().getEntities()) {
            if (entity == player || !entity.isAlive()) continue;
            if (!(entity instanceof LivingEntity living) || living.isDead()) continue;
            if (playersOnly.get() && !(entity instanceof PlayerEntity)) continue;
            if (entity.isInvisible() && !(entity instanceof PlayerEntity)) continue;

            double distSq = player.squaredDistanceTo(entity);
            if (distSq > maxSq) continue;

            // Prefer targets roughly in front of the player (silent but not completely blind)
            Vec3d toTarget = entity.getPos().subtract(player.getEyePos()).normalize();
            Vec3d look = player.getRotationVec(1.0F);
            if (look.dotProduct(toTarget) < 0.15) continue; // roughly in front cone

            if (distSq < bestDist) {
                bestDist = distSq;
                best = entity;
            }
        }
        return best;
    }

    private static boolean isWeapon(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = stack.getItem().toString().toLowerCase();
        return id.contains("sword") || id.contains("axe");
    }
}
