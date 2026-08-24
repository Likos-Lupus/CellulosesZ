package top.likoslupus.cellulosesz.common.admin;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

/**
 * Explicit punishment expiration; permanent punishments never use null or sentinel values.
 */
public sealed interface Expiration permits Expiration.Permanent, Expiration.At {

    static Expiration permanent() {
        return new Permanent();
    }

    static Expiration after(Instant now, Duration duration) {
        requireNonNull(now, "now");
        requirePositive(duration, "duration");

        try {
            return at(now.plus(duration));
        } catch (ArithmeticException | DateTimeException failure) {
            throw new IllegalArgumentException("expiration overflows Instant", failure);
        }
    }

    static Expiration at(Instant instant) {
        return new At(instant);
    }

    default Optional<Instant> expiresAt() {
        return this instanceof At(Instant instant)
                ? Optional.of(instant)
                : Optional.empty();
    }

    default boolean expired(Instant now) {
        requireNonNull(now, "now");
        return this instanceof At(Instant instant) && !instant.isAfter(now);
    }

    record Permanent() implements Expiration {

    }

    record At(
            Instant instant
    ) implements Expiration {

        public At {
            requireNonNull(instant, "instant");
        }

    }

}
