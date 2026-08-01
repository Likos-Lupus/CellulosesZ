package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.List;

final class ModuleScopedCommandMiddlewareRegistry implements CommandMiddlewareRegistry {

    private final String owner;
    private final CommandMiddlewareRegistry delegate;
    private final DefaultModuleScope scope;

    ModuleScopedCommandMiddlewareRegistry(
            String owner,
            CommandMiddlewareRegistry delegate,
            DefaultModuleScope scope
    ) {
        this.owner = owner;
        this.delegate = delegate;
        this.scope = scope;
    }

    @Override
    public Registration addMiddleware(CommandMiddleware middleware, String ignoredOwner) {
        return scope.own(delegate.addMiddleware(middleware, owner));
    }

    @Override
    public List<CommandMiddleware> middlewares() {
        return delegate.middlewares();
    }

}
