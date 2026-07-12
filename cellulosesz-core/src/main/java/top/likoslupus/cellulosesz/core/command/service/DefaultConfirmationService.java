package top.likoslupus.cellulosesz.core.command.service;

import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class DefaultConfirmationService implements ConfirmationService {

    private final Map<Key, Pending> pending = new ConcurrentHashMap<>();

    @Override
    public String request(
            UUID uuid,
            String action,
            Object payload,
            Duration ttl
    ) {
        var generated = Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        var token = generated.length() >= 6
                ? generated.substring(0, 6)
                : "0".repeat(6 - generated.length()) + generated;
        pending.put(
                new Key(uuid, action),
                new Pending(token, payload, System.currentTimeMillis() + Math.max(1L, ttl.toMillis()))
        );
        return token;
    }

    @Override
    public <T> Optional<T> consume(
            UUID uuid,
            String action,
            String token,
            Class<T> type
    ) {
        var key = new Key(uuid, action);
        var value = pending.get(key);

        if (value == null || value.expiresAt < System.currentTimeMillis() || !value.token.equals(token)) {
            if (value != null && value.expiresAt < System.currentTimeMillis()) {
                pending.remove(key, value);
            }
            return Optional.empty();
        }
        if (!type.isInstance(value.payload) || !pending.remove(key, value)) {
            return Optional.empty();
        }

        return Optional.of(type.cast(value.payload));
    }

    @Override
    public void clear(UUID uuid, String action) {
        pending.remove(new Key(uuid, action));
    }

    private record Key(
            UUID uuid,
            String action
    ) {

    }

    private record Pending(
            String token,
            Object payload,
            long expiresAt
    ) {

    }

}
