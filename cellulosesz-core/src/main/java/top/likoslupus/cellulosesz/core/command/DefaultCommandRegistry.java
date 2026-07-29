package top.likoslupus.cellulosesz.core.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandExecutionPipeline;
import top.likoslupus.cellulosesz.api.command.service.CommandAliasRegistry;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.core.command.execution.LegacyCommandPolicyContext;

import java.util.*;

public final class DefaultCommandRegistry implements CommandRegistry {

    private final PermissionCatalog permissionCatalog;
    private final CommandAliasRegistry aliasRegistry;
    private final CommandExecutionPipeline pipeline;
    private final Map<String, CellCommand> commands = new LinkedHashMap<>();
    private final Map<String, CellCommand> aliases = new LinkedHashMap<>();
    private final Map<CellCommand, String> moduleIds = new IdentityHashMap<>();
    private final Set<String> disabledCommands = new LinkedHashSet<>();

    public DefaultCommandRegistry(
            PermissionCatalog permissionCatalog,
            CommandAliasRegistry aliasRegistry,
            CommandExecutionPipeline pipeline
    ) {
        this.permissionCatalog = permissionCatalog;
        this.aliasRegistry = aliasRegistry;
        this.pipeline = pipeline;
    }

    public synchronized void disabledCommands(Collection<String> disabledCommands) {
        this.disabledCommands.clear();
        disabledCommands.stream()
                .map(this::normalize)
                .forEach(this.disabledCommands::add);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    public synchronized boolean disabled(String canonicalName) {
        return disabledCommands.contains(normalize(canonicalName));
    }

    @Override
    public synchronized void register(CellCommand command) {
        register("unknown", command);
    }

    @Override
    public synchronized void register(String moduleId, CellCommand command) {
        var name = normalize(command.name());
        if (commands.containsKey(name) || aliases.containsKey(name)) {
            throw new IllegalStateException("Command name is already registered: %s".formatted(command.name()));
        }

        commands.put(name, command);
        moduleIds.put(command, moduleId);
        permissionCatalog.register(command.permission(), command.description());
        aliasRegistry.register(name, command.aliases());

        command.aliases().forEach(alias -> {
            var normalizedAlias = normalize(alias);
            if (commands.containsKey(normalizedAlias) || aliases.containsKey(normalizedAlias)) {
                throw new IllegalStateException("Command alias is already registered: %s".formatted(alias));
            }
            aliases.put(normalizedAlias, command);
        });
    }

    @Override
    public synchronized Collection<CellCommand> commands() {
        return List.copyOf(commands.values());
    }

    @Override
    public synchronized Optional<CellCommand> command(String nameOrAlias) {
        var normalized = normalize(nameOrAlias);
        var command = commands.get(normalized);
        return command != null
                ? Optional.of(command)
                : Optional.ofNullable(aliases.get(normalized));
    }

    @Override
    public synchronized Optional<String> moduleId(CellCommand command) {
        return Optional.ofNullable(moduleIds.get(command));
    }

    @Override
    public int execute(CellCommand command, CommandInvocation invocation) {
        var descriptor = new CommandDescriptor(
                moduleId(command).orElse("unknown"),
                command.name(),
                command.permission(),
                command.sourceKind()
        );
        return pipeline.execute(
                descriptor,
                new LegacyCommandPolicyContext(descriptor, invocation),
                () -> command.execute(invocation)
        );
    }

}
