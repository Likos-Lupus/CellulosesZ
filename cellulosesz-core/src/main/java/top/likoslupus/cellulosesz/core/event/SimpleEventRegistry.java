package top.likoslupus.cellulosesz.core.event;

import top.likoslupus.cellulosesz.api.event.EventRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class SimpleEventRegistry implements EventRegistry {

    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    @Override
    public <T> void listen(Class<T> eventType, Consumer<T> listener) {
        requireNonNull(eventType, "eventType");
        requireNonNull(listener, "listener");
        listeners.computeIfAbsent(eventType, _ -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void fire(T event) {
        requireNonNull(event, "event");
        var eventListeners = listeners.getOrDefault(event.getClass(), List.of());
        eventListeners.forEach(listener -> ((Consumer<T>) listener).accept(event));
    }

}
