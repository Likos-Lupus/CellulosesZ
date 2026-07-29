package top.likoslupus.cellulosesz.api.command.catalog;

import java.util.Collection;

public interface CommandCatalog {

    Collection<CommandCatalogEntry> directCommands();

    void replaceDirect(Collection<CommandCatalogEntry> entries);

}
