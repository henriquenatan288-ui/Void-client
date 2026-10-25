package dev.voidclient.mixin;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import dev.voidclient.modules.combat.SlowHit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration()I", at = @At("HEAD"), cancellable = true)
    private void voidclient$slowLocalSwing(CallbackInfoReturnable<Integer> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || (Object) this != client.player) return;
        if (VoidClient.MODULE_MANAGER == null) return;

        Module module = VoidClient.MODULE_MANAGER.getByName("SlowHit");
        if (module instanceof SlowHit slowHit && slowHit.isEnabled()) {
            cir.setReturnValue(slowHit.getVisualSwingDuration());
        }
    }
}
