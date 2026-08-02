package top.likoslupus.cellulosesz.api.command.execution;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;

import static java.util.Objects.requireNonNull;

public record CommandOutcome(
        Status status,
        int brigadierResult
) {

    public CommandOutcome {
        requireNonNull(status, "status");
    }

    public static CommandOutcome fromSuccess(boolean success) {
        return success
                ? success()
                : rejected();
    }

    public static CommandOutcome success() {
        return success(1);
    }

    public static CommandOutcome rejected() {
        return rejected(0);
    }

    public static CommandOutcome success(int brigadierResult) {
        return new CommandOutcome(Status.SUCCESS, brigadierResult);
    }

    public static CommandOutcome rejected(int brigadierResult) {
        return new CommandOutcome(Status.REJECTED, brigadierResult);
    }

    public static CommandOutcome fromStatus(Status status) {
        requireNonNull(status, "status");
        return switch (status) {
            case SUCCESS -> success();
            case REJECTED -> rejected();
            case FAILED -> failed();
            case PARTIAL -> partial();
        };
    }

    public static CommandOutcome failed() {
        return failed(0);
    }

    public static CommandOutcome partial() {
        return partial(1);
    }

    public static CommandOutcome failed(int brigadierResult) {
        return new CommandOutcome(Status.FAILED, brigadierResult);
    }

    public static CommandOutcome partial(int brigadierResult) {
        return new CommandOutcome(Status.PARTIAL, brigadierResult);
    }

    public static CommandOutcome fromBrigadierResult(int result) {
        return result > 0
                ? success(result)
                : rejected(result);
    }

    public static CommandOutcome fromPlatformStatus(PlatformOperationStatus status) {
        requireNonNull(status, "status");
        return switch (status) {
            case SUCCESS -> success();
            case PARTIAL_SUCCESS -> partial();
            case WRONG_THREAD,
                 NOT_READY,
                 STORAGE_FAILURE,
                 ROLLBACK_FAILED,
                 INTERNAL_ERROR -> failed();
            default -> rejected();
        };
    }

    public boolean successful() {
        return status == Status.SUCCESS;
    }

    public enum Status {
        SUCCESS,
        REJECTED,
        FAILED,
        PARTIAL
    }

}
