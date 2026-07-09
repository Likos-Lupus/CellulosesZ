package top.likoslupus.cellulosesz.core.command;

import top.likoslupus.cellulosesz.api.command.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultCommandRegistry implements CommandRegistry, CommandMiddlewareRegistry {

    private final Map<String, CellCommand> commands = new LinkedHashMap<>();
    private final Map<String, CellCommand> aliases = new LinkedHashMap<>();
    private final Map<CellCommand, String> moduleIds = new IdentityHashMap<>();
    private final Set<String> disabledCommands = new LinkedHashSet<>();
    private final List<CommandMiddleware> middlewares = new CopyOnWriteArrayList<>();

    public synchronized void disabledCommands(Collection<String> disabledCommands) {
        this.disabledCommands.clear();
        disabledCommands.stream()
                .map(this::normalize)
                .forEach(this.disabledCommands::add);
    }

    @Override
    public synchronized void register(CellCommand command) {
        register("unknown", command);
    }

    @Override
    public void addMiddleware(CommandMiddleware middleware) {
        middlewares.add(middleware);
    }

    @Override
    public synchronized void register(String moduleId, CellCommand command) {
        var name = normalize(command.name());
        if (disabledCommands.contains(name)) return;

        if (commands.containsKey(name) || aliases.containsKey(name)) {
            throw new IllegalStateException("Command name is already registered: %s".formatted(command.name()));
        }

        commands.put(name, command);
        moduleIds.put(command, moduleId);

        command.aliases().forEach(alias -> {
            var normalizedAlias = normalize(alias);
            if (disabledCommands.contains(normalizedAlias)) {
                return;
            }
            if (commands.containsKey(normalizedAlias) || aliases.containsKey(normalizedAlias)) {
                throw new IllegalStateException("Command alias is already registered: %s".formatted(alias));
            }
            aliases.put(normalizedAlias, command);
        });
    }

    @Override
    public List<CommandMiddleware> middlewares() {
        return List.copyOf(middlewares);
    }

    private int invoke(
            CellCommand command,
            CommandInvocation invocation,
            int index
    ) {
        if (index >= middlewares.size()) {
            return command.execute(invocation);
        }
        var middleware = middlewares.get(index);
        return middleware.invoke(
                command,
                invocation,
                () -> invoke(command, invocation, index + 1)
        );
    }

    @Override
    public synchronized Collection<CellCommand> commands() {
        return List.copyOf(commands.values());
    }

    @Override
    public synchronized Optional<CellCommand> command(String nameOrAlias) {
        var normalized = normalize(nameOrAlias);
        var command = commands.get(normalized);

        if (command != null) return Optional.of(command);
        return Optional.ofNullable(aliases.get(normalized));
    }

    @Override
    public synchronized Optional<String> moduleId(CellCommand command) {
        return Optional.ofNullable(moduleIds.get(command));
    }


    @Override
    public int execute(CellCommand command, CommandInvocation invocation) {
        return invoke(command, invocation, 0);
    }


    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

}
