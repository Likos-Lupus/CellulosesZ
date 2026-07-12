package top.likoslupus.cellulosesz.api.event;

import java.util.function.Consumer;

public interface EventRegistry {

    <T> void listen(Class<T> eventType, Consumer<T> listener);

    default <T extends CancellableEvent> boolean fireCancellable(T event) {
        fire(event);
        return !event.cancelled();
    }

    <T> void fire(T event);

}
