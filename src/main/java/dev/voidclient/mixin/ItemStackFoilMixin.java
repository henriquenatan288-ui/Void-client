package dev.voidclient.mixin;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import dev.voidclient.modules.combat.SlowHit;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackFoilMixin {
    @Inject(method = "hasFoil()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void voidclient$slowHitFoil(CallbackInfoReturnable<Boolean> cir) {
        if (VoidClient.MODULE_MANAGER == null) return;
        Module mod = VoidClient.MODULE_MANAGER.getByName("SlowHit");
        if (!(mod instanceof SlowHit sh) || !sh.isEnabled() || !sh.enchantedLook()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack self = (ItemStack) (Object) this;
        if (self == mc.player.getMainHandItem() || self == mc.player.getOffhandItem()) {
            cir.setReturnValue(true);
        }
    }
}
