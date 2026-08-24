package top.likoslupus.cellulosesz.core.command.service;

import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultCooldownService implements CooldownService {

    private final ServiceRegistry services;
    private final Map<Key, Long> transientCooldowns = new ConcurrentHashMap<>();

    public DefaultCooldownService(ServiceRegistry services) {
        this.services = services;
    }

    private record Key(
            UUID uuid,
            String key
    ) {

    }

    @Override
    public Duration remaining(UUID uuid, String key) {
        var expiresAt = stored(uuid, key);
        var remaining = Math.max(0L, expiresAt - System.currentTimeMillis());

        if (remaining == 0L) {
            clear(uuid, key);
        }
        return Duration.ofMillis(remaining);
    }

    @Override
    public boolean ready(UUID uuid, String key) {
        return remaining(uuid, key).isZero();
    }

    @Override
    public void start(
            UUID uuid,
            String key,
            Duration duration
    ) {
        var expiresAt = System.currentTimeMillis() + Math.max(0L, duration.toMillis());
        transientCooldowns.put(new Key(uuid, key), expiresAt);
        var users = services.find(UserService.class);
        if (users != null && users.cached(uuid) != null) {
            users.updateVoid(
                    uuid,
                    user -> {
                        var cooldowns = new LinkedHashMap<>(user.cooldowns());
                        cooldowns.put(key, expiresAt);
                        return user.withCooldowns(cooldowns);
                    }
            );
        }
    }

    @Override
    public void clear(UUID uuid, String key) {
        transientCooldowns.remove(new Key(uuid, key));
        var users = services.find(UserService.class);
        if (users != null) {
            var user = users.cached(uuid);
            if (user != null && user.cooldowns().containsKey(key)) {
                users.updateVoid(
                        uuid,
                        current -> {
                            var cooldowns = new LinkedHashMap<>(current.cooldowns());
                            cooldowns.remove(key);
                            return current.withCooldowns(cooldowns);
                        }
                );
            }
        }
    }

    private long stored(UUID uuid, String key) {
        var transientValue = transientCooldowns.getOrDefault(new Key(uuid, key), 0L);
        var users = services.find(UserService.class);
        var user = users != null
                ? users.cached(uuid)
                : null;
        var persisted = user != null
                ? user.cooldowns().getOrDefault(key, 0L)
                : 0L;
        return Math.max(transientValue, persisted);
    }

}
