package top.likoslupus.cellulosesz.api.service;

import java.util.Optional;

public interface ServiceRegistry {

    <T> void register(Class<T> type, T instance);

    <T> T require(Class<T> type);

    <T> Optional<T> optional(Class<T> type);

    boolean contains(Class<?> type);

}
