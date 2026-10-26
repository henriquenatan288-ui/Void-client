package dev.voidclient.modules.personalization;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;

/**
 * Global HUD / interface colors (FPS, ArrayList, Watermark defaults).
 * Individual HUD modules can still override with their own Color setting.
 */
public class HudTheme extends Module {
    private final ColorSetting primary = addSetting(
            new ColorSetting("Primary", "Default HUD text color", 0xFFAA00FF));
    private final ColorSetting secondary = addSetting(
            new ColorSetting("Secondary", "Secondary HUD color", 0xFFFFFFFF));
    private final ColorSetting background = addSetting(
            new ColorSetting("Background", "Optional HUD background tint", 0x80000000));
    private final BooleanSetting forceGlobal = addSetting(
            new BooleanSetting("Force Global", "Override per-module HUD colors", false));

    public HudTheme() {
        super("HudTheme", "Colors for FPS and other interface elements.", Category.PERSONALIZATION);
        setEnabled(true);
    }

    public static HudTheme get() {
        if (VoidClient.MODULE_MANAGER == null) return null;
        var mod = VoidClient.MODULE_MANAGER.getByName("HudTheme");
        return mod instanceof HudTheme theme ? theme : null;
    }

    public int primary() { return primary.get(); }
    public int secondary() { return secondary.get(); }
    public int background() { return background.get(); }
    public boolean forceGlobal() { return forceGlobal.get(); }

    /** Color to use for a HUD element; respects Force Global. */
    public static int resolve(int moduleLocalColor) {
        HudTheme theme = get();
        if (theme == null || !theme.isEnabled()) return moduleLocalColor;
        if (theme.forceGlobal()) return theme.primary();
        return moduleLocalColor;
    }
}
