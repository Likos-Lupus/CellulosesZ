package top.likoslupus.cellulosesz.api.command.service;

public enum CommandDispatchStatus {

    EXECUTED,
    PERMISSION_DENIED,
    UNKNOWN_COMMAND,
    SYNTAX_ERROR,
    REJECTED_BY_GUARD,
    NOT_READY,
    INTERNAL_ERROR;

    public boolean successful() {
        return this == EXECUTED;
    }

}
