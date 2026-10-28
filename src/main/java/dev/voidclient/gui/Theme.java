package dev.voidclient.gui;

/**
 * Black / purple ClickGUI theme (MC 26.2).
 * Static fields for new GUI + method aliases so ModuleSettingsScreen still compiles.
 */
public final class Theme {

    private Theme() {}

    // Backgrounds
    public static final int BG_OVERLAY     = 0x50000000;
    public static final int PANEL_BG       = 0xE8090611;
    public static final int BG_SIDEBAR     = 0xF00B0714;
    public static final int BG_PANEL       = 0xF0100A1C;
    public static final int BG_CARD        = 0xF0151021;
    public static final int BG_CARD_HOVER  = 0xF01E1630;
    public static final int BG_INPUT       = 0xF0161024;
    public static final int BG_INPUT_FOCUS = 0xF01F1636;
    public static final int BG_SELECTED    = 0xF02B1A52;

    // Text
    public static final int TEXT_PRIMARY   = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFB9AECF;
    public static final int TEXT_MUTED     = 0xFF6F6684;

    // Borders / accents
    public static final int BORDER         = 0xFF2A2140;
    public static final int BORDER_STRONG  = 0xFF3B2D5C;
    public static final int ACCENT         = 0xFFA855F7;
    public static final int ACCENT_DIM     = 0x55A855F7;

    // Module list
    public static final int MODULE_ON      = 0xFF7C3AED;
    public static final int MODULE_HOVER   = 0xA0301C5C;
    public static final int SCROLL_THUMB   = 0xAA7C3AED;

    // Toggle
    public static final int TOGGLE_ON      = 0xFF7C3AED;
    public static final int TOGGLE_OFF     = 0xFF2E2640;
    public static final int TOGGLE_KNOB_ON = 0xFFFFFFFF;
    public static final int TOGGLE_KNOB_OFF= 0xFFCFC6E0;

    // Slider
    public static final int SLIDER_TRACK   = 0xFF2E2640;
    public static final int SLIDER_FILL    = 0xFFA855F7;

    // ---- method aliases (ModuleSettingsScreen / legacy) ----
    public static int BG_OVERLAY() { return BG_OVERLAY; }
    public static int BG_SIDEBAR() { return BG_SIDEBAR; }
    public static int BG_PANEL() { return BG_PANEL; }
    public static int BG_CARD() { return BG_CARD; }
    public static int BG_CARD_HOVER() { return BG_CARD_HOVER; }
    public static int BG_INPUT() { return BG_INPUT; }
    public static int BG_SELECTED() { return BG_SELECTED; }
    public static int TEXT_PRIMARY() { return TEXT_PRIMARY; }
    public static int TEXT_SECONDARY() { return TEXT_SECONDARY; }
    public static int TEXT_MUTED() { return TEXT_MUTED; }
    public static int BORDER() { return BORDER; }
    public static int BORDER_STRONG() { return BORDER_STRONG; }
    public static int ACCENT() { return ACCENT; }
    public static int ACCENT_DIM() { return ACCENT_DIM; }
    public static int TOGGLE_ON() { return TOGGLE_ON; }
    public static int TOGGLE_OFF() { return TOGGLE_OFF; }
    public static int TOGGLE_KNOB_ON() { return TOGGLE_KNOB_ON; }
    public static int TOGGLE_KNOB_OFF() { return TOGGLE_KNOB_OFF; }
    public static int SLIDER_TRACK() { return SLIDER_TRACK; }
    public static int SLIDER_FILL() { return SLIDER_FILL; }
}
