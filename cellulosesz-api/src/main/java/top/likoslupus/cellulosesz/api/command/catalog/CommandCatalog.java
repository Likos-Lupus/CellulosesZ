package top.likoslupus.cellulosesz.api.command.catalog;

import java.util.Collection;
import java.util.stream.Stream;

public interface CommandCatalog {

    default Collection<CommandCatalogEntry> commands() {
        return Stream.concat(
                directCommands().stream(),
                legacyCommands().stream()
        ).toList();
    }

    Collection<CommandCatalogEntry> directCommands();

    Collection<CommandCatalogEntry> legacyCommands();

    void replaceDirect(Collection<CommandCatalogEntry> entries);

    void replaceLegacy(Collection<CommandCatalogEntry> entries);

}
