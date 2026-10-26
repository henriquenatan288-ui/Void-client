package dev.voidclient.modules.misc;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import net.minecraft.client.Minecraft;

/** Hides your real name in client UI when wired to render hooks. */
public class NameProtect extends Module {
    private final ModeSetting alias = addSetting(new ModeSetting(
            "Alias", "Replacement name", "Player", "Player", "Void", "Hidden"));

    public NameProtect() {
        super("NameProtect", "Replace your name with an alias.", Category.MISC);
    }

    public static boolean isActive() {
        if (dev.voidclient.VoidClient.MODULE_MANAGER == null) return false;
        var mod = dev.voidclient.VoidClient.MODULE_MANAGER.getByName("NameProtect");
        return mod != null && mod.isEnabled();
    }

    public static String protect(String original) {
        if (!isActive()) return original;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || original == null) return original;

        String real;
        try {
            real = mc.player.getGameProfile().name();
        } catch (Throwable t) {
            try {
                real = mc.player.getName().getString();
            } catch (Throwable t2) {
                return original;
            }
        }
        if (real == null || real.isEmpty() || !original.contains(real)) return original;

        var mod = dev.voidclient.VoidClient.MODULE_MANAGER.getByName("NameProtect");
        String replacement = mod instanceof NameProtect np ? np.alias.get() : "Player";
        return original.replace(real, replacement);
    }
}
