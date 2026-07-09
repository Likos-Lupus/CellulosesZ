package top.likoslupus.cellulosesz.core.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;

import java.util.Collection;
import java.util.Optional;

public final class ModuleScopedCommandRegistry implements CommandRegistry {

    private final String moduleId;
    private final CommandRegistry delegate;

    public ModuleScopedCommandRegistry(
            String moduleId,
            CommandRegistry delegate
    ) {
        this.moduleId = moduleId;
        this.delegate = delegate;
    }

    @Override
    public void register(String moduleId, CellCommand command) {
        delegate.register(moduleId, command);
    }

    @Override
    public void register(CellCommand command) {
        delegate.register(moduleId, command);
    }

    @Override
    public Collection<CellCommand> commands() {
        return delegate.commands();
    }

    @Override
    public Optional<CellCommand> command(String nameOrAlias) {
        return delegate.command(nameOrAlias);
    }

    @Override
    public Optional<String> moduleId(CellCommand command) {
        return delegate.moduleId(command);
    }

    @Override
    public int execute(CellCommand command, CommandInvocation invocation) {
        return delegate.execute(command, invocation);
    }

}
