package top.likoslupus.cellulosesz.api.platform;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record NativeCommandResult(
        Status status,
        int resultCode,
        String detail
) {

    public NativeCommandResult {
        requireNonNull(status, "status");
        detail = requireNonNull(detail, "detail").trim();
    }

    public static NativeCommandResult success(int resultCode) {
        return new NativeCommandResult(Status.SUCCESS, resultCode, "");
    }

    public static NativeCommandResult notAvailable(String detail) {
        return new NativeCommandResult(Status.NOT_AVAILABLE, 0, detail);
    }

    public static NativeCommandResult parseFailure(String detail) {
        return new NativeCommandResult(Status.PARSE_FAILURE, 0, detail);
    }

    public static NativeCommandResult executionFailure(int resultCode, String detail) {
        return new NativeCommandResult(Status.EXECUTION_FAILURE, resultCode, detail);
    }

    public boolean success() {
        return status == Status.SUCCESS && resultCode > 0;
    }

    public Optional<String> failureDetail() {
        return detail.isEmpty()
                ? Optional.empty()
                : Optional.of(detail);
    }

    public enum Status {

        SUCCESS,
        NOT_AVAILABLE,
        PARSE_FAILURE,
        EXECUTION_FAILURE

    }

}
