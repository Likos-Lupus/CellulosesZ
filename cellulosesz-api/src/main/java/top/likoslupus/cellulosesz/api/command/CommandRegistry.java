package top.likoslupus.cellulosesz.api.command;

import java.util.Collection;
import java.util.Optional;

public interface CommandRegistry {

    void register(CellCommand command);

    Collection<CellCommand> commands();

    Optional<CellCommand> command(String nameOrAlias);

}
