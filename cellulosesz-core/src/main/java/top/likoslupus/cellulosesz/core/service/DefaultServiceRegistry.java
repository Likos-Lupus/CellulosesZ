package top.likoslupus.cellulosesz.core.service;

import top.likoslupus.cellulosesz.api.service.ServiceRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultServiceRegistry implements ServiceRegistry {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> type, T instance) {
        services.put(type, type.cast(instance));
    }

    @Override
    public <T> T require(Class<T> type) {
        return optional(type).orElseThrow(() -> new IllegalStateException("Required service is not registered: " + type.getName()));
    }

    @Override
    public <T> Optional<T> optional(Class<T> type) {
        var value = services.get(type);
        if (value == null) return Optional.empty();
        return Optional.of(type.cast(value));
    }

    @Override
    public boolean contains(Class<?> type) {
        return services.containsKey(type);
    }

}
