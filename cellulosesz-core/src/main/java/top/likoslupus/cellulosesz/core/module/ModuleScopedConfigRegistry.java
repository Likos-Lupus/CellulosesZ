package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.core.config.ConfigRegistry;

import java.util.Optional;
import java.util.function.Supplier;

final class ModuleScopedConfigRegistry implements ConfigRegistry {

    private final String owner;
    private final ConfigRegistry delegate;
    private final DefaultModuleScope scope;

    ModuleScopedConfigRegistry(
            String owner,
            ConfigRegistry delegate,
            DefaultModuleScope scope
    ) {
        this.owner = owner;
        this.delegate = delegate;
        this.scope = scope;
    }

    @Override
    public Registration register(
            String key,
            Class<?> type,
            String relativePath,
            Supplier<?> defaultSupplier,
            String ignoredOwner
    ) {
        return scope.own(delegate.register(
                key,
                type,
                relativePath,
                defaultSupplier,
                owner
        ));
    }

    @Override
    public <T> T require(String key, Class<T> type) {
        return delegate.require(key, type);
    }

    @Override
    public <T> Optional<T> optional(String key, Class<T> type) {
        return delegate.optional(key, type);
    }

    @Override
    public void reload() {
        delegate.reload();
    }

}
