package top.likoslupus.cellulosesz.api.command.service;

import java.time.Duration;
import java.util.UUID;

public interface CooldownService {

    Duration remaining(UUID uuid, String key);

    boolean ready(UUID uuid, String key);

    void start(
            UUID uuid,
            String key,
            Duration duration
    );

    void clear(UUID uuid, String key);

}
