package top.likoslupus.cellulosesz.core.command.catalog;

import java.util.Collection;

public interface CommandCatalog {

    Collection<CommandCatalogEntry> commands();

    void replace(Collection<CommandCatalogEntry> entries);

}
