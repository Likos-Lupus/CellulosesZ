package top.likoslupus.cellulosesz.core.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;

import java.util.*;

public final class DefaultCommandRegistry implements CommandRegistry {

    private final Map<String, CellCommand> commands = new LinkedHashMap<>();
    private final Map<String, CellCommand> aliases = new LinkedHashMap<>();

    @Override
    public synchronized void register(CellCommand command) {
        var name = normalize(command.name());
        if (commands.containsKey(name) || aliases.containsKey(name)) {
            throw new IllegalStateException("Command name is already registered: %s".formatted(command.name()));
        }

        commands.put(name, command);
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

        if (command != null) return Optional.of(command);
        return Optional.ofNullable(aliases.get(normalized));
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

}
