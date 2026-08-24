package top.likoslupus.cellulosesz.core.command.catalog;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record CommandCatalogEntry(
        CommandDescriptor descriptor,
        List<String> aliases,
        String description,
        String usage
) {

    public CommandCatalogEntry {
        requireNonNull(descriptor, "descriptor");
        aliases = List.copyOf(requireNonNull(aliases, "aliases"));
        description = requireNonNull(description, "description");
        usage = requireNonNull(usage, "usage");
    }

}
