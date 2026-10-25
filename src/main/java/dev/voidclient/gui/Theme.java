package dev.voidclient.gui;

/**
 * Centralized colors for the ClickGUI (dark neon).
 */
public final class Theme {

    private Theme() {}

    // Backgrounds
    public static final int BG_DARK        = 0xF00B0B12;
    public static final int BG_PANEL       = 0xF012121A;
    public static final int BG_CARD        = 0xF01A1A24;
    public static final int BG_CARD_HOVER  = 0xF0222230;
    public static final int BG_SIDEBAR     = 0xF00E0E16;
    public static final int BG_INPUT       = 0xF0161620;

    // Accent (purple neon)
    public static final int ACCENT         = 0xFF8B5CF6;
    public static final int ACCENT_HOVER   = 0xFFA78BFA;
    public static final int ACCENT_DIM     = 0xAA8B5CF6;

    // Text
    public static final int TEXT_PRIMARY   = 0xFFF5F5F7;
    public static final int TEXT_SECONDARY = 0xFF9CA3AF;
    public static final int TEXT_MUTED     = 0xFF6B7280;

    // Borders / separators
    public static final int BORDER         = 0xFF2A2A35;
    public static final int BORDER_ACCENT  = 0xFF8B5CF6;

    // Toggle
    public static final int TOGGLE_ON      = 0xFF8B5CF6;
    public static final int TOGGLE_OFF     = 0xFF3F3F4A;
    public static final int TOGGLE_KNOB    = 0xFFFFFFFF;

    // Misc
    public static final int SUCCESS        = 0xFF22C55E;
    public static final int DANGER         = 0xFFEF4444;
    public static final int OVERLAY        = 0x88000000;
}
