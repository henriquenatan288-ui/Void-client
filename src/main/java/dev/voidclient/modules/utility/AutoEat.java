package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Eats food when hunger is low. NoSlow while eating via InputMixin. */
public class AutoEat extends Module {
    private final NumberSetting hunger = addSetting(
            new NumberSetting("Hunger", "Eat when food level is at or below this", 14, 1, 19, 1));

    private boolean eating;

    public AutoEat() {
        super("AutoEat", "Eat food automatically (no slowdown while eating).", Category.UTILITY);
    }

    public static boolean shouldCancelUseSlowdown() {
        if (dev.voidclient.VoidClient.MODULE_MANAGER == null) return false;
        var mod = dev.voidclient.VoidClient.MODULE_MANAGER.getByName("AutoEat");
        return mod instanceof AutoEat eat && eat.isEnabled() && eat.eating;
    }

    @Override
    protected void onDisable() {
        eating = false;
        stopUsing();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null || mc.level == null) {
            eating = false;
            return;
        }

        if (p.getFoodData().getFoodLevel() > hunger.getInt()) {
            if (eating) stopUsing();
            eating = false;
            return;
        }

        if (p.isUsingItem()) {
            eating = isFood(p.getUseItem());
            return;
        }

        if (tryUse(p, InteractionHand.MAIN_HAND)) return;
        if (tryUse(p, InteractionHand.OFF_HAND)) return;

        // Only eat from current hotbar / hands for API stability on 26.2
        eating = false;
    }

    private boolean tryUse(LocalPlayer p, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = p.getItemInHand(hand);
        if (!isFood(stack)) return false;
        mc.gameMode.useItem(p, hand);
        eating = true;
        return true;
    }

    private void stopUsing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isUsingItem()) {
            mc.player.stopUsingItem();
        }
    }

    private static boolean isFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            return stack.get(net.minecraft.core.component.DataComponents.FOOD) != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
