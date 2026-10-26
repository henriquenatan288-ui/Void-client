package dev.voidclient.core.setting;

/** Stores color as ARGB int (0xAARRGGBB). */
public class ColorSetting extends Setting<Integer> {
    private static final int[] PALETTE = {
            0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55,
            0xFF55FF55, 0xFF00FFAA, 0xFF55FFFF,
            0xFF5599FF, 0xFFAA00FF, 0xFFFFFFFF
    };

    public ColorSetting(String name, String description, int defaultArgb) {
        super(name, description, defaultArgb);
    }

    public int getRgb() {
        return get() & 0x00FFFFFF;
    }

    public int getAlpha() {
        return (get() >> 24) & 0xFF;
    }

    public float[] getFloatComponents() {
        int c = get();
        return new float[]{
                ((c >> 16) & 0xFF) / 255f,
                ((c >> 8) & 0xFF) / 255f,
                (c & 0xFF) / 255f,
                ((c >> 24) & 0xFF) / 255f
        };
    }

    public static int paletteSize() {
        return PALETTE.length;
    }

    public static int paletteColor(int index) {
        return PALETTE[index];
    }

    public static int paletteIndex(int color) {
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == color) return i;
        }
        return -1;
    }

    public void cyclePalette() {
        int index = paletteIndex(get());
        set(PALETTE[(index + 1 + PALETTE.length) % PALETTE.length]);
    }

    @Override
    public Object toConfigValue() {
        return get();
    }

    @Override
    public void fromConfigValue(Object raw) {
        if (raw instanceof Number n) set(n.intValue());
    }
}
