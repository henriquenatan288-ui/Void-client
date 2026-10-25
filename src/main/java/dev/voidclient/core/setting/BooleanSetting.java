package dev.voidclient.core.setting;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public boolean isEnabled() {
        return get();
    }

    public void toggle() {
        set(!get());
    }

    @Override
    public Object toConfigValue() {
        return get();
    }

    @Override
    public void fromConfigValue(Object raw) {
        if (raw instanceof Boolean b) {
            set(b);
        }
    }
}
