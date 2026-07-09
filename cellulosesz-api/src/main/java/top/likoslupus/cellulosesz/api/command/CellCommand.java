package top.likoslupus.cellulosesz.api.command;

import java.util.List;

public interface CellCommand {

    default List<String> aliases() {
        return List.of();
    }

    default String permission() {
        return "";
    }

    default CommandSourceKind sourceKind() {
        return CommandSourceKind.ANY;
    }

    default String description() {
        return "";
    }

    default String usage() {
        return "/" + name();
    }

    String name();

    int execute(CommandInvocation invocation);

}
