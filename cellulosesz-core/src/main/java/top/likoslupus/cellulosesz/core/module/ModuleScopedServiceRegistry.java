package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;

import java.util.*;

final class ModuleScopedServiceRegistry implements ServiceRegistry {

    private final String moduleId;
    private final ServiceRegistry delegate;
    private final List<Registration> registrations = new ArrayList<>();
    private final Set<Object> observed = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<AsyncInitializable> initializables = new ArrayList<>();
    private final List<AsyncCloseable> closeables = new ArrayList<>();

    ModuleScopedServiceRegistry(String moduleId, ServiceRegistry delegate) {
        this.moduleId = moduleId;
        this.delegate = delegate;
    }

    @Override
    public synchronized <T> Registration register(Class<T> type, T instance, String ignoredOwner) {
        var registration = delegate.register(type, instance, moduleId);
        registrations.add(registration);
        if (observed.add(instance)) {
            if (instance instanceof AsyncInitializable initializable) initializables.add(initializable);
            if (instance instanceof AsyncCloseable closeable) closeables.add(closeable);
        }
        return registration;
    }

    @Override
    public <T> T require(Class<T> type) {
        return delegate.require(type);
    }

    @Override
    public <T> Optional<T> optional(Class<T> type) {
        return delegate.optional(type);
    }

    @Override
    public boolean contains(Class<?> type) {
        return delegate.contains(type);
    }

    synchronized List<AsyncInitializable> initializables() {
        return List.copyOf(initializables);
    }

    synchronized List<AsyncCloseable> closeablesInReverseOrder() {
        var copy = new ArrayList<>(closeables);
        Collections.reverse(copy);
        return List.copyOf(copy);
    }

    synchronized List<Registration> registrationsInReverseOrder() {
        var copy = new ArrayList<>(registrations);
        Collections.reverse(copy);
        return List.copyOf(copy);
    }

}
