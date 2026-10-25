package dev.voidclient.core.setting;

/**
 * Base class for all module settings.
 * Settings are automatically saved/loaded by ConfigManager.
 */
public abstract class Setting<T> {

    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public T getDefault() {
        return defaultValue;
    }

    public void reset() {
        this.value = defaultValue;
    }

    /** Used by ConfigManager for serialization. */
    public abstract Object toConfigValue();

    /** Used by ConfigManager for deserialization. */
    public abstract void fromConfigValue(Object raw);
}
