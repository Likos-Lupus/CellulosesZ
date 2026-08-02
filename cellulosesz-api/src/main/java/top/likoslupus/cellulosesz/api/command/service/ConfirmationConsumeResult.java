package top.likoslupus.cellulosesz.api.command.service;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/** Distinguishes normal confirmation misses from token, expiry, and type failures. */
public final class ConfirmationConsumeResult<T> {

    private final ConfirmationConsumeStatus status;
    private final Optional<T> payload;

    private ConfirmationConsumeResult(
            ConfirmationConsumeStatus status,
            Optional<T> payload
    ) {
        this.status = requireNonNull(status, "status");
        this.payload = requireNonNull(payload, "payload");
        if ((status == ConfirmationConsumeStatus.CONSUMED) != payload.isPresent()) {
            throw new IllegalArgumentException("Only consumed confirmations carry a payload");
        }
    }

    public static <T> ConfirmationConsumeResult<T> consumed(T payload) {
        return new ConfirmationConsumeResult<>(
                ConfirmationConsumeStatus.CONSUMED,
                Optional.of(requireNonNull(payload, "payload"))
        );
    }

    public static <T> ConfirmationConsumeResult<T> failure(ConfirmationConsumeStatus status) {
        if (status == ConfirmationConsumeStatus.CONSUMED) {
            throw new IllegalArgumentException("Use consumed for successful confirmation");
        }
        return new ConfirmationConsumeResult<>(status, Optional.empty());
    }

    public ConfirmationConsumeStatus status() {
        return status;
    }

    public Optional<T> payload() {
        return payload;
    }

    public boolean consumed() {
        return status == ConfirmationConsumeStatus.CONSUMED;
    }

}
