package top.likoslupus.cellulosesz.api.command.service;

import static java.util.Objects.requireNonNull;

public record PlayerCommandDispatchResult(
        CommandDispatchStatus status,
        int commandResult,
        String detail
) {

    public PlayerCommandDispatchResult {
        requireNonNull(status, "status");
        detail = requireNonNull(detail, "detail");
        if (!status.successful() && commandResult != 0) {
            throw new IllegalArgumentException("Failure results cannot contain an executed result code");
        }
    }

    public static PlayerCommandDispatchResult executed(int commandResult) {
        return new PlayerCommandDispatchResult(CommandDispatchStatus.EXECUTED, commandResult, "");
    }

    public static PlayerCommandDispatchResult failure(CommandDispatchStatus status, String detail) {
        if (status.successful()) throw new IllegalArgumentException("Failure status must not be successful");
        return new PlayerCommandDispatchResult(status, 0, detail);
    }

    public boolean successful() {
        return status.successful();
    }

}
