package top.likoslupus.cellulosesz.api.command.service;

import java.time.Duration;
import java.util.UUID;

public interface ConfirmationService {

    <T> ConfirmationToken request(
            UUID uuid,
            ConfirmationKey<T> key,
            T payload,
            Duration ttl
    );

    <T> ConfirmationConsumeResult<T> consume(
            UUID uuid,
            ConfirmationKey<T> key,
            ConfirmationToken token
    );

    <T> void clear(UUID uuid, ConfirmationKey<T> key);

}
