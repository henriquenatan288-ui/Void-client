package dev.voidclient.mixin;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import dev.voidclient.modules.render.PlayerESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class PlayerOutlineMixin {
    @Inject(method = "hasOutline(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void voidclient$outlineEspTargets(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (VoidClient.MODULE_MANAGER == null) return;
        Module module = VoidClient.MODULE_MANAGER.getByName("PlayerESP");
        if (module instanceof PlayerESP playerEsp && playerEsp.isTarget(entity)) cir.setReturnValue(true);
    }
}
