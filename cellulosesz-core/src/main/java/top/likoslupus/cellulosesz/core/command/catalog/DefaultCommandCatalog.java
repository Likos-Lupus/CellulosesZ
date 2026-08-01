package top.likoslupus.cellulosesz.core.command.catalog;

import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalog;
import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalogEntry;

import java.util.Collection;
import java.util.List;

public final class DefaultCommandCatalog implements CommandCatalog {

    private volatile List<CommandCatalogEntry> entries = List.of();

    @Override
    public Collection<CommandCatalogEntry> commands() {
        return entries;
    }

    @Override
    public void replace(Collection<CommandCatalogEntry> entries) {
        this.entries = List.copyOf(entries);
    }

}
