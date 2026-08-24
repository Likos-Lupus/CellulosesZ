package top.likoslupus.cellulosesz.core.service;

import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class DefaultServiceRegistry implements ServiceRegistry {

    private final Map<Class<?>, ServiceEntry> services = new ConcurrentHashMap<>();

    @Override
    public <T> Registration register(Class<T> type, T instance, String owner) {
        requireNonNull(type, "type");
        requireNonNull(instance, "instance");
        requireNonBlank(owner, "owner");
        var entry = new ServiceEntry(type.cast(instance), owner);
        var existing = services.putIfAbsent(type, entry);
        if (existing != null) {
            throw new IllegalStateException("Service is already registered: %s (owner=%s)".formatted(
                    type.getName(),
                    existing.owner()
            ));
        }
        return new ServiceRegistration(type, entry);
    }

    @Override
    public <T> T require(Class<T> type) {
        var service = find(type);
        if (service == null) {
            throw new IllegalStateException(
                    "Required service is not registered: " + type.getName());
        }
        return service;
    }

    @Override
    public <T> @Nullable T find(Class<T> type) {
        requireNonNull(type, "type");
        var entry = services.get(type);
        if (entry == null) {
            return null;
        }
        return type.cast(entry.instance());
    }

    @Override
    public boolean contains(Class<?> type) {
        return services.containsKey(type);
    }

    private record ServiceEntry(
            Object instance,
            String owner
    ) {

    }

    private final class ServiceRegistration implements Registration {

        private final Class<?> type;
        private final ServiceEntry entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ServiceRegistration(
                Class<?> type,
                ServiceEntry entry
        ) {
            this.type = type;
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
                services.remove(type, entry);
            }
        }

    }

}
