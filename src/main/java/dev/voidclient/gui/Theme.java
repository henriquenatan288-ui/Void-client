package dev.voidclient.gui;

import dev.voidclient.modules.personalization.GuiTheme;

/**
 * Black & white modern theme for Void Client ClickGUI.
 * Values can be overridden live by the GuiTheme (Personalization) module.
 */
public final class Theme {

    private Theme() {}

    // Fallbacks when GuiTheme is not loaded
    private static final int DEF_BG_OVERLAY     = 0xB0000000;
    private static final int DEF_BG_SIDEBAR     = 0xF00A0A0A;
    private static final int DEF_BG_PANEL       = 0xF0101010;
    private static final int DEF_BG_CARD        = 0xF0141414;
    private static final int DEF_BG_CARD_HOVER  = 0xF01C1C1C;
    private static final int DEF_BG_INPUT       = 0xF0181818;
    private static final int DEF_BG_SELECTED    = 0xF0282828;
    private static final int DEF_TEXT_PRIMARY   = 0xFFFFFFFF;
    private static final int DEF_TEXT_SECONDARY = 0xFFB0B0B0;
    private static final int DEF_TEXT_MUTED     = 0xFF707070;
    private static final int DEF_BORDER         = 0xFF2A2A2A;
    private static final int DEF_BORDER_STRONG  = 0xFF3A3A3A;
    private static final int DEF_ACCENT         = 0xFFFFFFFF;
    private static final int DEF_ACCENT_DIM     = 0x55FFFFFF;
    private static final int DEF_TOGGLE_ON      = 0xFFFFFFFF;
    private static final int DEF_TOGGLE_OFF     = 0xFF333333;
    private static final int DEF_TOGGLE_KNOB_ON = 0xFF0A0A0A;
    private static final int DEF_TOGGLE_KNOB_OFF= 0xFFCCCCCC;
    private static final int DEF_SLIDER_TRACK   = 0xFF2A2A2A;
    private static final int DEF_SLIDER_FILL    = 0xFFFFFFFF;

    private static GuiTheme theme() {
        return GuiTheme.get();
    }

    public static int BG_OVERLAY() { return DEF_BG_OVERLAY; }

    public static int BG_SIDEBAR() {
        GuiTheme t = theme();
        return t != null ? t.sidebar() : DEF_BG_SIDEBAR;
    }

    public static int BG_PANEL() {
        GuiTheme t = theme();
        return t != null ? t.panel() : DEF_BG_PANEL;
    }

    public static int BG_CARD() {
        GuiTheme t = theme();
        return t != null ? t.card() : DEF_BG_CARD;
    }

    public static int BG_CARD_HOVER() {
        int base = BG_CARD();
        // Slightly lighten for hover
        return lighten(base, 12);
    }

    public static int BG_INPUT() { return DEF_BG_INPUT; }
    public static int BG_SELECTED() { return DEF_BG_SELECTED; }

    public static int TEXT_PRIMARY() {
        GuiTheme t = theme();
        return t != null ? t.text() : DEF_TEXT_PRIMARY;
    }

    public static int TEXT_SECONDARY() {
        GuiTheme t = theme();
        return t != null ? t.textMuted() : DEF_TEXT_SECONDARY;
    }

    public static int TEXT_MUTED() { return DEF_TEXT_MUTED; }

    public static int BORDER() {
        GuiTheme t = theme();
        return t != null ? t.border() : DEF_BORDER;
    }

    public static int BORDER_STRONG() { return DEF_BORDER_STRONG; }

    public static int ACCENT() {
        GuiTheme t = theme();
        return t != null ? t.accent() : DEF_ACCENT;
    }

    public static int ACCENT_DIM() {
        int a = ACCENT();
        return (0x55 << 24) | (a & 0x00FFFFFF);
    }

    public static int TOGGLE_ON() { return ACCENT(); }
    public static int TOGGLE_OFF() { return DEF_TOGGLE_OFF; }
    public static int TOGGLE_KNOB_ON() { return DEF_TOGGLE_KNOB_ON; }
    public static int TOGGLE_KNOB_OFF() { return DEF_TOGGLE_KNOB_OFF; }
    public static int SLIDER_TRACK() { return BORDER(); }
    public static int SLIDER_FILL() { return ACCENT(); }

    // Legacy static fields kept for any code still referencing constants directly
    public static final int BG_OVERLAY = DEF_BG_OVERLAY;
    public static final int BG_SIDEBAR = DEF_BG_SIDEBAR;
    public static final int BG_PANEL = DEF_BG_PANEL;
    public static final int BG_CARD = DEF_BG_CARD;
    public static final int BG_CARD_HOVER = DEF_BG_CARD_HOVER;
    public static final int BG_INPUT = DEF_BG_INPUT;
    public static final int BG_SELECTED = DEF_BG_SELECTED;
    public static final int TEXT_PRIMARY = DEF_TEXT_PRIMARY;
    public static final int TEXT_SECONDARY = DEF_TEXT_SECONDARY;
    public static final int TEXT_MUTED = DEF_TEXT_MUTED;
    public static final int BORDER = DEF_BORDER;
    public static final int BORDER_STRONG = DEF_BORDER_STRONG;
    public static final int ACCENT = DEF_ACCENT;
    public static final int ACCENT_DIM = DEF_ACCENT_DIM;
    public static final int TOGGLE_ON = DEF_TOGGLE_ON;
    public static final int TOGGLE_OFF = DEF_TOGGLE_OFF;
    public static final int TOGGLE_KNOB_ON = DEF_TOGGLE_KNOB_ON;
    public static final int TOGGLE_KNOB_OFF = DEF_TOGGLE_KNOB_OFF;
    public static final int SLIDER_TRACK = DEF_SLIDER_TRACK;
    public static final int SLIDER_FILL = DEF_SLIDER_FILL;

    private static int lighten(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + amount);
        int b = Math.min(255, (argb & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
