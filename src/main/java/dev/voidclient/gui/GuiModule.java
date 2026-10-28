package dev.voidclient.gui;

import dev.voidclient.core.Category;

/**
 * Bridge between Module and ClickGUI.
 */
public interface GuiModule {

    String name();

    Category category();

    boolean isEnabled();

    /** Left click. */
    void toggle();

    /** Right click — open settings. */
    default void openSettings() {}
}
