package dev.voidclient.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Simple typed event bus.
 * Listeners are registered by event class.
 */
public class EventBus {

    private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> listeners = new ConcurrentHashMap<>();

    public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void post(T event) {
        List<Consumer<? extends Event>> list = listeners.get(event.getClass());
        if (list == null) return;

        for (Consumer<? extends Event> listener : list) {
            ((Consumer<T>) listener).accept(event);
            if (event.isCancelled()) break;
        }
    }

    public void clear() {
        listeners.clear();
    }
}
