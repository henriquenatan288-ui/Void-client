package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Automatically switches to the best tool in the hotbar for the block being mined.
 */
public class AutoTool extends Module {
    private final BooleanSetting switchBack = addSetting(
            new BooleanSetting("Switch back", "Return to previous slot when not mining", true));

    private int previousSlot = -1;
    private boolean wasMining;

    public AutoTool() {
        super("AutoTool", "Switch to the best hotbar tool when mining.", Category.UTILITY);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null) return;

        boolean mining = mc.options.attackKey.isPressed()
                && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;

        if (!mining) {
            if (wasMining && switchBack.get() && previousSlot != -1) {
                player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
            wasMining = false;
            return;
        }

        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit)) return;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return;

        PlayerInventory inv = player.getInventory();
        int bestSlot = -1;
        float bestSpeed = 1.0f;

        // Only consider hotbar (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            // Prefer tools that are actually effective
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != -1 && bestSlot != inv.getSelectedSlot()) {
            if (!wasMining) {
                previousSlot = inv.getSelectedSlot();
            }
            inv.setSelectedSlot(bestSlot);
        }
        wasMining = true;
    }

    @Override
    protected void onDisable() {
        if (switchBack.get() && previousSlot != -1) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
            }
        }
        previousSlot = -1;
        wasMining = false;
    }
}
