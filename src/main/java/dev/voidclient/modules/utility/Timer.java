package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;

/**
 * Client-side game speed multiplier.
 * Applied via mixin on the timer / delta tracker when available.
 */
public class Timer extends Module {
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Game speed multiplier", 1.5, 0.1, 5.0, 0.1));

    public Timer() {
        super("Timer", "Change client game speed.", Category.UTILITY);
    }

    public float getSpeed() {
        return isEnabled() ? speed.getFloat() : 1.0f;
    }

    public static float currentSpeed() {
        if (dev.voidclient.VoidClient.MODULE_MANAGER == null) return 1.0f;
        var mod = dev.voidclient.VoidClient.MODULE_MANAGER.getByName("Timer");
        return mod instanceof Timer timer ? timer.getSpeed() : 1.0f;
    }
}
