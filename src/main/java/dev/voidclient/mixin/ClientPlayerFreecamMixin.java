package dev.voidclient.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Previously cancelled sendPosition during freecam.
 * New FreecamController leaves the player frozen via input only and does not
 * cancel movement packets (avoids onGround desync / slow break issues).
 * Mixin kept as a no-op marker so mixins.json stays stable.
 */
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerFreecamMixin {
}
