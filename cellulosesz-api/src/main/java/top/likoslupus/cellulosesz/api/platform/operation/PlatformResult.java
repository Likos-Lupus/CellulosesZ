package top.likoslupus.cellulosesz.api.platform.operation;

import java.util.Optional;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Strongly constrained platform operation outcome. */
public final class PlatformResult<T> {

    private final PlatformOperationStatus status;
    private final Optional<T> value;
    private final String detail;

    private PlatformResult(
            PlatformOperationStatus status,
            Optional<T> value,
            String detail
    ) {
        this.status = requireNonNull(status, "status");
        this.value = requireNonNull(value, "value");
        this.detail = requireNonNull(detail, "detail");
        var successful = status == PlatformOperationStatus.SUCCESS
                || status == PlatformOperationStatus.PARTIAL_SUCCESS;
        if (!successful && value.isPresent()) {
            throw new IllegalArgumentException("Failure results cannot carry a value");
        }
        if (status == PlatformOperationStatus.PARTIAL_SUCCESS) {
            requireNonBlank(detail, "detail");
        } else if (status == PlatformOperationStatus.SUCCESS && !detail.isEmpty()) {
            throw new IllegalArgumentException("Successful results cannot carry failure detail");
        } else if (!successful) {
            requireNonBlank(detail, "detail");
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
        return new PlatformResult<>(PlatformOperationStatus.SUCCESS, Optional.empty(), "");
    }

    public static <T> PlatformResult<T> partial(T value, String detail) {
        return new PlatformResult<>(
                PlatformOperationStatus.PARTIAL_SUCCESS,
                Optional.of(requireNonNull(value, "value")),
                detail
        );
    }

    public static <T> PlatformResult<T> failure(PlatformOperationStatus status, String detail) {
        if (status == PlatformOperationStatus.SUCCESS
                || status == PlatformOperationStatus.PARTIAL_SUCCESS
        ) {
            throw new IllegalArgumentException(
                    "Use success or partial factories for successful results");
        }
        return new PlatformResult<>(status, Optional.empty(), detail);
    }

    public PlatformOperationStatus status() {
        return status;
    }

    public Optional<T> value() {
        return value;
    }

    public String detail() {
        return detail;
    }

    public boolean successful() {
        return status == PlatformOperationStatus.SUCCESS
                || status == PlatformOperationStatus.PARTIAL_SUCCESS;
    }

}
