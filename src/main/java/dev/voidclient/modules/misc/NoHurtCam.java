package dev.voidclient.modules.misc;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;

/**
 * Disables hurt camera shake.
 * Applied via mixin on GameRenderer hurt cam when wired.
 */
public class NoHurtCam extends Module {
    public NoHurtCam() {
        super("NoHurtCam", "Remove damage camera shake.", Category.MISC);
    }

    public static boolean isActive() {
        if (dev.voidclient.VoidClient.MODULE_MANAGER == null) return false;
        var mod = dev.voidclient.VoidClient.MODULE_MANAGER.getByName("NoHurtCam");
        return mod != null && mod.isEnabled();
    }
}
