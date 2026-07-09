package top.likoslupus.cellulosesz.api.command;

public interface CommandMiddleware {

    int invoke(
            CellCommand command,
            CommandInvocation invocation,
            CommandContinuation continuation
    );

}
