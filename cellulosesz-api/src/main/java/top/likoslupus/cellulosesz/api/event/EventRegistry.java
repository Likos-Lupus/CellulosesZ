package top.likoslupus.cellulosesz.api.event;

import java.util.function.Consumer;

public interface EventRegistry {

    <T> void listen(Class<T> eventType, Consumer<T> listener);

    <T> void fire(T event);

}
