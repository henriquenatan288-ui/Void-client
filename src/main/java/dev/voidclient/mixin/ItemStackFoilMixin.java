package dev.voidclient.mixin;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import dev.voidclient.modules.combat.SlowHit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces the enchanted glint (foil) on the item currently held by the local player
 * when SlowHit "Enchanted Look" is enabled.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackFoilMixin {

    @Inject(method = "hasFoil()Z", at = @At("HEAD"), cancellable = true)
    private void voidclient$forceFoil(CallbackInfoReturnable<Boolean> cir) {
        if (VoidClient.MODULE_MANAGER == null) return;

        Module mod = VoidClient.MODULE_MANAGER.getByName("SlowHit");
        if (!(mod instanceof SlowHit slowHit) || !slowHit.shouldForceFoil()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) return;

        // Only force foil on the item the player is currently holding (main or off hand)
        if (self == player.getMainHandStack() || self == player.getOffHandStack()) {
            cir.setReturnValue(true);
        }
    }
}
