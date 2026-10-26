package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Drops junk items from the inventory. */
public class InvCleaner extends Module {
    private final NumberSetting delay = addSetting(
            new NumberSetting("Delay", "Ticks between drops", 2, 0, 10, 1));
    private int cooldown;

    private static final Item[] JUNK = {
            Items.COBBLESTONE,
            Items.NETHERRACK,
            Items.DIRT,
            Items.GRAVEL,
            Items.ANDESITE,
            Items.DIORITE,
            Items.GRANITE,
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.POISONOUS_POTATO
    };

    public InvCleaner() {
        super("InvCleaner", "Drop common junk items from inventory.", Category.UTILITY);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) return;
        if (p.containerMenu != p.inventoryMenu) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.isEmpty() || !isJunk(stack)) continue;

            int containerSlot = i < 9 ? i + 36 : i;
            mc.gameMode.handleContainerInput(
                    p.inventoryMenu.containerId, containerSlot, 1, ContainerInput.THROW, p);
            cooldown = delay.getInt();
            return;
        }
    }

    private static boolean isJunk(ItemStack stack) {
        for (Item item : JUNK) {
            if (stack.is(item)) return true;
        }
        return false;
    }
}
