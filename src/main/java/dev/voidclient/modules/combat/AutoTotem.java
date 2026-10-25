package dev.voidclient.modules.combat;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;
        if (p.currentScreenHandler != p.playerScreenHandler) return;

        ItemStack off = p.getOffHandStack();
        if (off.isOf(Items.TOTEM_OF_UNDYING)) return;
        if (soft.get() && !off.isEmpty()) return;

        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) {
                slot = i;
                break;
            }
        }
        if (slot == -1) return;

        // Convert hotbar/main to container slot id
        int containerSlot = slot < 9 ? slot + 36 : slot;

        // Pick up totem
        mc.interactionManager.clickSlot(
                p.playerScreenHandler.syncId, containerSlot, 0, SlotActionType.PICKUP, p);
        // Place in offhand (slot 45)
        mc.interactionManager.clickSlot(
                p.playerScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, p);
        // Put back whatever was in cursor
        if (!p.currentScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(
                    p.playerScreenHandler.syncId, containerSlot, 0, SlotActionType.PICKUP, p);
        }
    }
}
