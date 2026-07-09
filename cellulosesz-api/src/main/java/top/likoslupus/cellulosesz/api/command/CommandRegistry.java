package top.likoslupus.cellulosesz.api.command;

import java.util.Collection;
import java.util.Optional;

public interface CommandRegistry {

    default void register(String moduleId, CellCommand command) {
        register(command);
    }

    void register(CellCommand command);

    Collection<CellCommand> commands();

    Optional<CellCommand> command(String nameOrAlias);

    default Optional<String> moduleId(CellCommand command) {
        return Optional.empty();
    }

    default int execute(CellCommand command, CommandInvocation invocation) {
        return command.execute(invocation);
    }

}
