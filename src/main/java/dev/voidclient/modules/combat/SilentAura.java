package dev.voidclient.modules.combat;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Attacks nearest valid target without rotating the player body. */
public class SilentAura extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 3.2, 2.0, 6.0, 0.1));
    private final BooleanSetting players = addSetting(new BooleanSetting("Players", "Target players", true));
    private final BooleanSetting hostiles = addSetting(new BooleanSetting("Hostiles", "Target monsters", true));
    private final BooleanSetting animals = addSetting(new BooleanSetting("Animals", "Target passive mobs", false));
    private final BooleanSetting swing = addSetting(new BooleanSetting("Swing", "Client arm swing", false));
    private final BooleanSetting cooldown = addSetting(new BooleanSetting("Cooldown", "Wait for full attack strength", true));

    public SilentAura() {
        super("SilentAura", "Attack nearest target without turning.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) return;
        if (player.isSpectator() || !player.isAlive()) return;
        if (cooldown.get() && player.getAttackStrengthScale(0.5f) < 1.0f) return;

        LivingEntity target = findTarget(mc, player);
        if (target == null) return;

        try {
            mc.gameMode.attack(player, target);
            if (swing.get()) player.swing(InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {
        }
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer player) {
        double r = range.get();
        AABB box = player.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestD = r * r;

        for (Entity e : mc.level.getEntities(player, box, this::valid)) {
            if (!(e instanceof LivingEntity living)) continue;
            double d = player.distanceToSqr(living);
            if (d < bestD) {
                bestD = d;
                best = living;
            }
        }
        return best;
    }

    private boolean valid(Entity e) {
        if (!(e instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) return false;
        if (e instanceof Player) return players.get();
        if (e instanceof Monster) return hostiles.get();
        return animals.get();
    }
}
