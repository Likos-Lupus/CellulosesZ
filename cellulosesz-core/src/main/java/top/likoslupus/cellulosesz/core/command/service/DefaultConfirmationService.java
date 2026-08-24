package top.likoslupus.cellulosesz.core.command.service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class DefaultConfirmationService implements ConfirmationService {

    private final Map<Key, Pending<?>> pending = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final Supplier<String> tokenSource;

    public DefaultConfirmationService() {
        this(
                System::currentTimeMillis,
                () -> Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36)
        );
    }

    DefaultConfirmationService(
            LongSupplier clock,
            Supplier<String> tokenSource
    ) {
        this.clock = requireNonNull(clock, "clock");
        this.tokenSource = requireNonNull(tokenSource, "tokenSource");
    }

    @Override
    public <T> ConfirmationToken request(
            UUID uuid,
            ConfirmationKey<T> key,
            T payload,
            Duration ttl
    ) {
        requireNonNull(uuid, "uuid");
        requireNonNull(key, "key");
        requireNonNull(payload, "payload");
        requireNonNull(ttl, "ttl");

        if (!key.payloadType().isInstance(payload)) {
            throw new IllegalArgumentException("Payload does not match confirmation key type");
        }

        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }

        var generated = requireNonNull(tokenSource.get(), "generated token");
        var value = generated.length() >= 6
                ? generated.substring(0, 6)
                : "0".repeat(6 - generated.length()) + generated;
        var token = new ConfirmationToken(value);

        pending.put(
                new Key(uuid, key.id()),
                new Pending<>(
                        key,
                        token,
                        payload,
                        Math.addExact(clock.getAsLong(), ttl.toMillis())
                )
        );

        return token;
    }

    @Override
    public <T> ConfirmationConsumeResult<T> consume(
            UUID uuid,
            ConfirmationKey<T> key,
            ConfirmationToken token
    ) {
        requireNonNull(uuid, "uuid");
        requireNonNull(key, "key");
        requireNonNull(token, "token");

        var storageKey = new Key(uuid, key.id());
        var value = pending.get(storageKey);

        if (value == null) {
            return ConfirmationConsumeResult.failure(ConfirmationConsumeStatus.NOT_FOUND);
        }

        if (value.expiresAt <= clock.getAsLong()) {
            pending.remove(storageKey, value);
            return ConfirmationConsumeResult.failure(ConfirmationConsumeStatus.EXPIRED);
        }

        if (!value.token.equals(token)) {
            return ConfirmationConsumeResult.failure(ConfirmationConsumeStatus.TOKEN_MISMATCH);
        }

        if (!value.key.payloadType().equals(key.payloadType()) || !key
                .payloadType()
                .isInstance(value.payload)
        ) {
            return ConfirmationConsumeResult.failure(ConfirmationConsumeStatus.PAYLOAD_TYPE_MISMATCH);
        }

        if (!pending.remove(storageKey, value)) {
            return ConfirmationConsumeResult.failure(ConfirmationConsumeStatus.CONCURRENTLY_CONSUMED);
        }

        return ConfirmationConsumeResult.consumed(key.payloadType().cast(value.payload));
    }

    @Override
    public <T> void clear(UUID uuid, ConfirmationKey<T> key) {
        pending.remove(new Key(
                requireNonNull(uuid, "uuid"),
                requireNonNull(key, "key").id()
        ));
    }

    private record Key(
            UUID uuid,
            String id
    ) {

    }

    private record Pending<T>(
            ConfirmationKey<T> key,
            ConfirmationToken token,
            T payload,
            long expiresAt
    ) {

    }

}
