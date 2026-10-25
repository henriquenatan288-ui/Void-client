package dev.voidclient.core.setting;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = Arrays.asList(modes);
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default mode must be one of the available modes");
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public void cycle() {
        int index = modes.indexOf(get());
        int next = (index + 1) % modes.size();
        set(modes.get(next));
    }

    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    @Override
    public Object toConfigValue() {
        return get();
    }

    @Override
    public void fromConfigValue(Object raw) {
        if (raw instanceof String s && modes.contains(s)) {
            set(s);
        }
    }
}
