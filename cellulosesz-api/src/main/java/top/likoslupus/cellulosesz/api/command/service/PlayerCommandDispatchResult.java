package top.likoslupus.cellulosesz.api.command.service;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;

import static java.util.Objects.requireNonNull;

public record PlayerCommandDispatchResult(
        PlatformOperationStatus status,
        int commandResult,
        String detail
) {

    public PlayerCommandDispatchResult {
        requireNonNull(status, "status");
        detail = requireNonNull(detail, "detail");
    }

    public boolean successful() {
        return status == PlatformOperationStatus.SUCCESS;
    }

}
