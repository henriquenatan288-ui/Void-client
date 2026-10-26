package dev.voidclient.modules.personalization;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;

/**
 * ClickGUI color customization.
 * Always "active" for reading colors; enable only gates optional previews.
 */
public class GuiTheme extends Module {
    private final ColorSetting accent = addSetting(
            new ColorSetting("Accent", "Main accent (toggles, sliders, highlights)", 0xFFFFFFFF));
    private final ColorSetting sidebar = addSetting(
            new ColorSetting("Sidebar", "Sidebar background", 0xF00A0A0A));
    private final ColorSetting panel = addSetting(
            new ColorSetting("Panel", "Main panel background", 0xF0101010));
    private final ColorSetting card = addSetting(
            new ColorSetting("Card", "Module card background", 0xF0141414));
    private final ColorSetting text = addSetting(
            new ColorSetting("Text", "Primary text color", 0xFFFFFFFF));
    private final ColorSetting textMuted = addSetting(
            new ColorSetting("Text Muted", "Secondary / muted text", 0xFFB0B0B0));
    private final ColorSetting border = addSetting(
            new ColorSetting("Border", "Borders and dividers", 0xFF2A2A2A));

    public GuiTheme() {
        super("GuiTheme", "Customize ClickGUI colors.", Category.PERSONALIZATION);
        setEnabled(true);
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
