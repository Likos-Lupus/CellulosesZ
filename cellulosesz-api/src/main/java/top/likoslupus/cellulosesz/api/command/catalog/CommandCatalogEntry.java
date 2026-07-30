package top.likoslupus.cellulosesz.api.command.catalog;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record CommandCatalogEntry(
        CommandDescriptor descriptor,
        List<String> aliases,
        String description,
        String usage,
        CommandMigrationMode migrationMode
) {

    public CommandCatalogEntry(
            CommandDescriptor descriptor,
            List<String> aliases,
            String description,
            String usage
    ) {
        this(descriptor, aliases, description, usage, CommandMigrationMode.DIRECT);
    }

    public CommandCatalogEntry {
        descriptor = requireNonNull(descriptor, "descriptor");
        aliases = List.copyOf(requireNonNull(aliases, "aliases"));
        description = requireNonNull(description, "description");
        usage = requireNonNull(usage, "usage");
        migrationMode = requireNonNull(migrationMode, "migrationMode");
    }

}
