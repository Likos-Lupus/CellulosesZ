package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.function.Consumer;

public interface EventRegistry {

    default <T> Registration listen(
            Class<T> eventType,
            Consumer<T> listener
    ) {
        return listen(eventType, listener, "global");
    }

    <T> Registration listen(
            Class<T> eventType,
            Consumer<T> listener,
            String owner
    );

    default <T extends CancellableEvent> boolean fireCancellable(T event) {
        fire(event);
        return !event.cancelled();
    }

    <T> void fire(T event);

}
