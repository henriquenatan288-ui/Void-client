package dev.voidclient.core.setting;

/**
 * Stores a GLFW key code.
 * Use -1 for "None".
 */
public class KeybindSetting extends Setting<Integer> {

    public KeybindSetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    public boolean isPressed(int key) {
        return get() != -1 && get() == key;
    }

    public boolean isNone() {
        return get() == -1;
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
