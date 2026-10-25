package dev.voidclient.gui;

/**
 * Black & white modern theme for Void Client ClickGUI.
 */
public final class Theme {

    private Theme() {}

    // Backgrounds
    public static final int BG_OVERLAY     = 0xB0000000;
    public static final int BG_SIDEBAR     = 0xF00A0A0A;
    public static final int BG_PANEL       = 0xF0101010;
    public static final int BG_CARD        = 0xF0141414;
    public static final int BG_CARD_HOVER  = 0xF01C1C1C;
    public static final int BG_INPUT       = 0xF0181818;
    public static final int BG_SELECTED    = 0xF0282828;

    // Text
    public static final int TEXT_PRIMARY   = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFB0B0B0;
    public static final int TEXT_MUTED     = 0xFF707070;

    // Borders / accents (white-based)
    public static final int BORDER         = 0xFF2A2A2A;
    public static final int BORDER_STRONG  = 0xFF3A3A3A;
    public static final int ACCENT         = 0xFFFFFFFF;
    public static final int ACCENT_DIM     = 0x55FFFFFF;

    // Toggle
    public static final int TOGGLE_ON      = 0xFFFFFFFF;
    public static final int TOGGLE_OFF     = 0xFF333333;
    public static final int TOGGLE_KNOB_ON = 0xFF0A0A0A;
    public static final int TOGGLE_KNOB_OFF= 0xFFCCCCCC;

    // Slider
    public static final int SLIDER_TRACK   = 0xFF2A2A2A;
    public static final int SLIDER_FILL    = 0xFFFFFFFF;
}
