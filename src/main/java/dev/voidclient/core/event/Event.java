package dev.voidclient.core.event;

/**
 * Marker / base for all events.
 */
public abstract class Event {
    private boolean cancelled = false;

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }
}
