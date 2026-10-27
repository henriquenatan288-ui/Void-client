package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Switches to the best hotbar tool while mining. */
public class AutoTool extends Module {
    private final BooleanSetting onlyOnMine = addSetting(
            new BooleanSetting("Only while mining", "Switch only when holding attack", true));
    private final BooleanSetting switchBack = addSetting(
            new BooleanSetting("Switch back", "Return to previous slot when done", false));

    private int previousSlot = -1;

    public AutoTool() {
        super("AutoTool", "Auto-select best tool for the block you mine.", Category.UTILITY);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) return;
        if (player.isCreative() || player.isSpectator()) return;

        boolean mining = mc.options.keyAttack.isDown();
        if (onlyOnMine.get() && !mining) {
            maybeSwitchBack(player);
            return;
        }

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            if (!mining) maybeSwitchBack(player);
            return;
        }

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return;

        int best = bestSlot(player, state);
        if (best < 0) return;

        Inventory inv = player.getInventory();
        int current = inv.getSelectedSlot();
        if (current == best) return;

        if (switchBack.get() && previousSlot < 0) previousSlot = current;
        inv.setSelectedSlot(best);
    }

    private void maybeSwitchBack(LocalPlayer player) {
        if (!switchBack.get() || previousSlot < 0) return;
        player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
    }

    private static int bestSlot(LocalPlayer player, BlockState state) {
        Inventory inv = player.getInventory();
        int best = -1;
        float bestSpeed = 1.0f;
        ItemStack held = player.getMainHandItem();
        try {
            bestSpeed = held.getDestroySpeed(state);
        } catch (Throwable ignored) {}

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            float speed;
            try {
                speed = stack.getDestroySpeed(state);
            } catch (Throwable t) {
                continue;
            }
            if (speed > bestSpeed + 0.001f) {
                bestSpeed = speed;
                best = i;
            }
        }
        return best;
    }

    @Override
    protected void onDisable() {
        previousSlot = -1;
    }
}
