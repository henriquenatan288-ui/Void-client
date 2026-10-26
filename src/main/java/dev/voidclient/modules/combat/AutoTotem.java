package dev.voidclient.modules.combat;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Keeps a Totem of Undying in the offhand.
 */
public class AutoTotem extends Module {

    private final BooleanSetting soft = addSetting(
            new BooleanSetting("Soft", "Only swap if offhand empty", false));

    public AutoTotem() {
        super("AutoTotem", "Auto equip totem in offhand.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) return;
        if (p.containerMenu != p.inventoryMenu) return;

        ItemStack off = p.getOffhandItem();
        if (off.is(Items.TOTEM_OF_UNDYING)) return;
        if (soft.get() && !off.isEmpty()) return;

        Inventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.TOTEM_OF_UNDYING)) {
                slot = i;
                break;
            }
        }
        if (slot == -1) return;

        // Convert hotbar/main to container slot id
        int containerSlot = slot < 9 ? slot + 36 : slot;

        // Pick up totem
        mc.gameMode.handleInventoryMouseClick(
                p.inventoryMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
        // Place in offhand (slot 45)
        mc.gameMode.handleInventoryMouseClick(
                p.inventoryMenu.containerId, 45, 0, ClickType.PICKUP, p);
        // Put back whatever was in cursor
        if (!p.containerMenu.getCarried().isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(
                    p.inventoryMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
        }
    }
}
