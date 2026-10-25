package dev.voidclient.core.setting;

/**
 * Stores color as ARGB int (0xAARRGGBB).
 */
public class ColorSetting extends Setting<Integer> {

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

    @Override
    public Object toConfigValue() {
        return get();
    }

    @Override
    public void fromConfigValue(Object raw) {
        if (raw instanceof Number n) {
            set(n.intValue());
        }
    }
}
