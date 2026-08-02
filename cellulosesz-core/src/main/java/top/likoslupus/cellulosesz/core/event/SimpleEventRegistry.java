package top.likoslupus.cellulosesz.core.event;

import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class SimpleEventRegistry implements EventRegistry {

    private final Map<Class<?>, CopyOnWriteArrayList<ListenerEntry<?>>> listeners =
            new ConcurrentHashMap<>();

    @Override
    public <T> Registration listen(
            Class<T> eventType,
            Consumer<T> listener,
            String owner
    ) {
        requireNonNull(eventType, "eventType");
        requireNonNull(listener, "listener");
        requireNonBlank(owner, "owner");

        var entry = new ListenerEntry<>(listener, owner);
        listeners
                .computeIfAbsent(eventType, _ -> new CopyOnWriteArrayList<>())
                .add(entry);
        return new ListenerRegistration(eventType, entry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void fire(T event) {
        requireNonNull(event, "event");
        var eventListeners = listeners.getOrDefault(
                event.getClass(),
                new CopyOnWriteArrayList<>()
        );
        eventListeners.forEach(entry ->
                ((Consumer<T>) entry.listener()).accept(event)
        );
    }

    private void remove(Class<?> eventType, ListenerEntry<?> entry) {
        listeners.computeIfPresent(
                eventType,
                (_, entries) -> {
                    entries.removeIf(candidate -> candidate == entry);
                    return entries.isEmpty()
                            ? null
                            : entries;
                }
        );
    }

    private record ListenerEntry<T>(
            Consumer<T> listener,
            String owner
    ) {

    }

    private final class ListenerRegistration implements Registration {

        private final Class<?> eventType;
        private final ListenerEntry<?> entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ListenerRegistration(
                Class<?> eventType,
                ListenerEntry<?> entry
        ) {
            this.eventType = eventType;
            this.entry = entry;
        }

        @Override
        public String owner() {
            return entry.owner();
        }

        @Override
        public boolean closed() {
            return closed.get();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                remove(eventType, entry);
            }
        }

    }

}
