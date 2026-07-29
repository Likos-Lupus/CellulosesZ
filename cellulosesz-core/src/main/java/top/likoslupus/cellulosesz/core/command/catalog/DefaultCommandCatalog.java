package top.likoslupus.cellulosesz.core.command.catalog;

import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalog;
import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalogEntry;

import java.util.Collection;
import java.util.List;

public final class DefaultCommandCatalog implements CommandCatalog {

    private volatile List<CommandCatalogEntry> direct = List.of();

    @Override
    public Collection<CommandCatalogEntry> directCommands() {
        return direct;
    }

    @Override
    public void replaceDirect(Collection<CommandCatalogEntry> entries) {
        direct = List.copyOf(entries);
    }

}
