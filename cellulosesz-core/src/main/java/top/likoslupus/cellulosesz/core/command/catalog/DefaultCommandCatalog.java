package top.likoslupus.cellulosesz.core.command.catalog;

import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalog;
import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalogEntry;

import java.util.Collection;
import java.util.List;

public final class DefaultCommandCatalog implements CommandCatalog {

    private volatile List<CommandCatalogEntry> direct = List.of();
    private volatile List<CommandCatalogEntry> legacy = List.of();

    @Override
    public Collection<CommandCatalogEntry> directCommands() {
        return direct;
    }

    @Override
    public Collection<CommandCatalogEntry> legacyCommands() {
        return legacy;
    }

    @Override
    public void replaceDirect(Collection<CommandCatalogEntry> entries) {
        direct = List.copyOf(entries);
    }

    @Override
    public void replaceLegacy(Collection<CommandCatalogEntry> entries) {
        legacy = List.copyOf(entries);
    }

}
