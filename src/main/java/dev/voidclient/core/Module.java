package dev.voidclient.core;

import dev.voidclient.core.setting.Setting;
import java.util.ArrayList;
import java.util.List;

/** Base class for every module. */
public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int keybind;
    private final List<Setting<?>> settings = new ArrayList<>();
    private static long stateRevision;

    public static long getStateRevision() { return stateRevision; }

    protected Module(String name, String description, Category category) {
        this(name, description, category, -1);
    }

    protected Module(String name, String description, Category category, int defaultKeybind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keybind = defaultKeybind;
        this.enabled = false;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }
    public List<Setting<?>> getSettings() { return settings; }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public final void toggle() {
        if (enabled) disable();
        else enable();
    }

    public final void enable() {
        if (enabled) return;
        enabled = true;
        stateRevision++;
        onEnable();
    }

    public final void disable() {
        if (!enabled) return;
        enabled = false;
        stateRevision++;
        onDisable();
    }

    public final void setEnabled(boolean state) {
        if (state) enable();
        else disable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void onRender2D(float tickDelta) {}
}
