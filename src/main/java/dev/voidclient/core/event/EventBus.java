package dev.voidclient.core.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Typed event bus. Listeners are registered by event class.
 * Uses CopyOnWriteArrayList so subscribe/unsubscribe during post is safe.
 */
public class EventBus {

    private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> listeners = new ConcurrentHashMap<>();

    public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public <T extends Event> void unsubscribe(Class<T> eventClass, Consumer<T> listener) {
        List<Consumer<? extends Event>> list = listeners.get(eventClass);
        if (list == null) return;
        list.remove(listener);
        if (list.isEmpty()) listeners.remove(eventClass, list);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void post(T event) {
        List<Consumer<? extends Event>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) return;

        for (Consumer<? extends Event> listener : list) {
            ((Consumer<T>) listener).accept(event);
            if (event.isCancelled()) break;
        }
    }

    public void clear() {
        listeners.clear();
    }
}
