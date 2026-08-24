package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncCloseable;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncInitializable;

import java.util.*;
import org.jspecify.annotations.Nullable;

final class ModuleScopedServiceRegistry implements ServiceRegistry {

    private final String moduleId;
    private final ServiceRegistry delegate;
    private final DefaultModuleScope scope;
    private final Set<Object> observed = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<AsyncInitializable> initializables = new ArrayList<>();

    ModuleScopedServiceRegistry(
            String moduleId,
            ServiceRegistry delegate,
            DefaultModuleScope scope
    ) {
        this.moduleId = moduleId;
        this.delegate = delegate;
        this.scope = scope;
    }

    @Override
    public synchronized <T> Registration register(
            Class<T> type,
            T instance,
            String ignoredOwner
    ) {
        if (observed.add(instance)) {
            switch (instance) {
                case AsyncInitializable initializable -> initializables.add(initializable);
                case AsyncCloseable closeable -> scope.own(closeable);
                default -> {
                }
            }
        }

        return scope.own(delegate.register(type, instance, moduleId));
    }

    @Override
    public <T> T require(Class<T> type) {
        return delegate.require(type);
    }

    @Override
    public <T> @Nullable T find(Class<T> type) {
        return delegate.find(type);
    }

    @Override
    public boolean contains(Class<?> type) {
        return delegate.contains(type);
    }

    synchronized List<AsyncInitializable> initializables() {
        return List.copyOf(initializables);
    }

}
