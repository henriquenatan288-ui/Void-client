package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Quickly takes items from open chests/containers. */
public class ChestStealer extends Module {
    private final NumberSetting delay = addSetting(
            new NumberSetting("Delay", "Ticks between item takes", 1, 0, 10, 1));
    private int cooldown;

    public ChestStealer() {
        super("ChestStealer", "Steal items from opened chests.", Category.UTILITY);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) return;
        if (mc.screen == null) return;

        AbstractContainerMenu menu = p.containerMenu;
        if (!(menu instanceof ChestMenu chest)) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int chestSlots = chest.getRowCount() * 9;
        for (int i = 0; i < chestSlots; i++) {
            Slot slot = chest.getSlot(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            mc.gameMode.handleContainerInput(
                    chest.containerId, i, 0, ContainerInput.QUICK_MOVE, p);
            cooldown = delay.getInt();
            return;
        }
    }
}
