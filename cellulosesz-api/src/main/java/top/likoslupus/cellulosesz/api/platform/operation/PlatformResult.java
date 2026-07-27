package top.likoslupus.cellulosesz.api.platform.operation;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record PlatformResult<T>(
        PlatformOperationStatus status,
        Optional<T> value,
        String detail
) {

    public PlatformResult {
        requireNonNull(status, "status");
        requireNonNull(value, "value");
        detail = requireNonNull(detail, "detail");
        if (status == PlatformOperationStatus.SUCCESS && value.isEmpty() && !detail.isBlank()) {
            throw new IllegalArgumentException("A successful result without a value must not carry a failure detail");
        }
    }

    public static <T> PlatformResult<T> success(T value) {
        return new PlatformResult<>(
                PlatformOperationStatus.SUCCESS,
                Optional.of(requireNonNull(value, "value")),
                ""
        );
    }

    public static PlatformResult<Void> success() {
        return new PlatformResult<>(
                PlatformOperationStatus.SUCCESS,
                Optional.empty(),
                ""
        );
    }

    public static <T> PlatformResult<T> partial(T value, String detail) {
        return new PlatformResult<>(
                PlatformOperationStatus.PARTIAL_SUCCESS,
                Optional.of(requireNonNull(value, "value")),
                requireNonNull(detail, "detail")
        );
    }

    public static <T> PlatformResult<T> failure(PlatformOperationStatus status, String detail) {
        if (status == PlatformOperationStatus.SUCCESS || status == PlatformOperationStatus.PARTIAL_SUCCESS) {
            throw new IllegalArgumentException("Use success/partial factories for successful results");
        }
        return new PlatformResult<>(
                status,
                Optional.empty(),
                requireNonNull(detail, "detail")
        );
    }

    public boolean successful() {
        return status == PlatformOperationStatus.SUCCESS
                || status == PlatformOperationStatus.PARTIAL_SUCCESS;
    }

}
