package top.likoslupus.cellulosesz.core.event;

import top.likoslupus.cellulosesz.api.event.EventRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SimpleEventRegistry implements EventRegistry {

    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    @Override
    public <T> void listen(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, _ -> new ArrayList<>()).add(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void fire(T event) {
        var eventListeners = listeners.getOrDefault(event.getClass(), List.of());
        eventListeners.forEach(listener -> ((Consumer<T>) listener).accept(event));
    }

}
