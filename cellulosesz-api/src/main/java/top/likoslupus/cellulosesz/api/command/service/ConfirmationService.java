package top.likoslupus.cellulosesz.api.command.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface ConfirmationService {

    String request(
            UUID uuid,
            String action,
            Object payload,
            Duration ttl
    );

    <T> Optional<T> consume(
            UUID uuid,
            String action,
            String token,
            Class<T> type
    );

    void clear(UUID uuid, String action);

}
