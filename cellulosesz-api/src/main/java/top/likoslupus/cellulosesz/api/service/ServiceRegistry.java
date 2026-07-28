package top.likoslupus.cellulosesz.api.service;

import java.util.Optional;

public interface ServiceRegistry {

    default <T> Registration register(Class<T> type, T instance) {
        return register(type, instance, "global");
    }

    <T> Registration register(Class<T> type, T instance, String owner);

    <T> T require(Class<T> type);

    <T> Optional<T> optional(Class<T> type);

    boolean contains(Class<?> type);

}
