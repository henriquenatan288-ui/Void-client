package dev.voidclient.modules.personalization;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;

/**
 * ClickGUI color customization — dark glass defaults + reset.
 */
public class GuiTheme extends Module {
    // Dark glass style defaults (translucent blacks)
    public static final int DEF_ACCENT      = 0xFFE8E8E8;
    public static final int DEF_SIDEBAR     = 0xE0080808;
    public static final int DEF_PANEL       = 0xD00C0C0C;
    public static final int DEF_CARD        = 0xC0121212;
    public static final int DEF_TEXT        = 0xFFF0F0F0;
    public static final int DEF_TEXT_MUTED  = 0xFF9A9A9A;
    public static final int DEF_BORDER      = 0xFF1E1E1E;

    private final ColorSetting accent = addSetting(
            new ColorSetting("Accent", "Main accent (toggles, sliders)", DEF_ACCENT));
    private final ColorSetting sidebar = addSetting(
            new ColorSetting("Sidebar", "Sidebar background", DEF_SIDEBAR));
    private final ColorSetting panel = addSetting(
            new ColorSetting("Panel", "Main panel background", DEF_PANEL));
    private final ColorSetting card = addSetting(
            new ColorSetting("Card", "Module card background", DEF_CARD));
    private final ColorSetting text = addSetting(
            new ColorSetting("Text", "Primary text color", DEF_TEXT));
    private final ColorSetting textMuted = addSetting(
            new ColorSetting("Text Muted", "Secondary text", DEF_TEXT_MUTED));
    private final ColorSetting border = addSetting(
            new ColorSetting("Border", "Borders and dividers", DEF_BORDER));
    private final BooleanSetting reset = addSetting(
            new BooleanSetting("Reset Colors", "Restore default dark glass theme", false));

    public GuiTheme() {
        super("GuiTheme", "Customize ClickGUI colors (dark glass).", Category.PERSONALIZATION);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        if (reset.get()) {
            resetDefaults();
            reset.set(false);
            VoidClient.LOGGER.info("GuiTheme colors reset to dark glass defaults");
        }
    }

    public void resetDefaults() {
        accent.set(DEF_ACCENT);
        sidebar.set(DEF_SIDEBAR);
        panel.set(DEF_PANEL);
        card.set(DEF_CARD);
        text.set(DEF_TEXT);
        textMuted.set(DEF_TEXT_MUTED);
        border.set(DEF_BORDER);
    }

    public static GuiTheme get() {
        if (VoidClient.MODULE_MANAGER == null) return null;
        var mod = VoidClient.MODULE_MANAGER.getByName("GuiTheme");
        return mod instanceof GuiTheme theme ? theme : null;
    }

    public int accent() { return accent.get(); }
    public int sidebar() { return sidebar.get(); }
    public int panel() { return panel.get(); }
    public int card() { return card.get(); }
    public int text() { return text.get(); }
    public int textMuted() { return textMuted.get(); }
    public int border() { return border.get(); }
}
