package dev.voidclient.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses SimpleOption's stored value without invoking its vanilla validator. */
@Mixin(OptionInstance.class)
public interface SimpleOptionAccessor {
    @Accessor("value")
    void voidclient$setValueDirect(Object value);
}
