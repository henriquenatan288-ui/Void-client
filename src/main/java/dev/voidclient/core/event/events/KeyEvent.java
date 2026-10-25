package dev.voidclient.core.event.events;

import dev.voidclient.core.event.Event;

/**
 * Fired when a key is pressed or released.
 */
public class KeyEvent extends Event {

    private final int key;
    private final int scancode;
    private final int action; // 1 = press, 0 = release, 2 = repeat
    private final int modifiers;

    public KeyEvent(int key, int scancode, int action, int modifiers) {
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getKey() {
        return key;
    }

    public int getScancode() {
        return scancode;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isPress() {
        return action == 1;
    }

    public boolean isRelease() {
        return action == 0;
    }
}
