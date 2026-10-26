package dev.voidclient.modules.misc;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;

/** Disables every module instantly (emergency). */
public class Panic extends Module {
    public Panic() {
        super("Panic", "Disable all modules at once.", Category.MISC);
    }

    @Override
    protected void onEnable() {
        if (VoidClient.MODULE_MANAGER == null) {
            setEnabled(false);
            return;
        }
        for (Module module : VoidClient.MODULE_MANAGER.getModules()) {
            if (module == this) continue;
            if (module.isEnabled()) module.disable();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c[Void] §fPanic — all modules off"),
                    false);
        }
        setEnabled(false);
    }
}
