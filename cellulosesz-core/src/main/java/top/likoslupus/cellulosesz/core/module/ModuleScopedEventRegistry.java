package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.function.Consumer;

final class ModuleScopedEventRegistry implements EventRegistry {

    private final String owner;
    private final EventRegistry delegate;
    private final DefaultModuleScope scope;

    ModuleScopedEventRegistry(
            String owner,
            EventRegistry delegate,
            DefaultModuleScope scope
    ) {
        this.owner = owner;
        this.delegate = delegate;
        this.scope = scope;
    }

    @Override
    public <T> Registration listen(
            Class<T> eventType,
            Consumer<T> listener,
            String ignoredOwner
    ) {
        return scope.own(delegate.listen(eventType, listener, owner));
    }

    @Override
    public <T> void fire(T event) {
        delegate.fire(event);
    }

}
