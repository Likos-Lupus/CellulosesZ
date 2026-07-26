package top.likoslupus.cellulosesz.modules.playerstate.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultPlayerStateService implements PlayerStateService {

    private final PlatformService platform;
    private final UserService users;
    private final DisplayNameService displayNames;
    private final ConcurrentHashMap<UUID, Long> lastActivityNanos = new ConcurrentHashMap<>();

    public DefaultPlayerStateService(
            PlatformService platform,
            UserService users,
            DisplayNameService displayNames
    ) {
        this.platform = platform;
        this.users = users;
        this.displayNames = displayNames;
    }

    @Override
    public CompletableFuture<AdminResult> setFlying(CellPlayer player, boolean enabled) {
        return users.update(player.uuid(), user -> {
            var previous = user.state.flying;
            user.state.flying = enabled;
            return previous;
        }).thenCompose(previous -> platform.callOnServerThread(() -> platform.setFlying(player, enabled))
                .thenCompose(applied -> {
                    if (applied) return CompletableFuture.completedFuture(AdminResult.success(
                            enabled ? "service.playerstate.fly-enabled" : "service.playerstate.fly-disabled",
                            Map.of("player", displayNames.plainDisplayName(player))
                    ));
                    return users.updateVoid(player.uuid(), user -> {
                        if (user.state.flying == enabled) user.state.flying = previous;
                    }).handle((_, rollbackFailure) -> rollbackFailure == null
                            ? AdminResult.failure("service.playerstate.fly-failed")
                            : AdminResult.failure("service.user.rollback-failed"));
                })).exceptionally(_ -> AdminResult.failure("service.user.persistence-failed"));
    }

    @Override
    public CompletableFuture<AdminResult> setGod(CellPlayer player, boolean enabled) {
        return users.update(player.uuid(), user -> {
            var previous = user.state.god;
            user.state.god = enabled;
            return previous;
        }).thenCompose(previous -> platform.callOnServerThread(() -> platform.setInvulnerable(player, enabled))
                .thenCompose(applied -> {
                    if (applied) return CompletableFuture.completedFuture(AdminResult.success(
                            enabled ? "service.playerstate.god-enabled" : "service.playerstate.god-disabled",
                            Map.of("player", displayNames.plainDisplayName(player))
                    ));
                    return users.updateVoid(player.uuid(), user -> {
                        if (user.state.god == enabled) user.state.god = previous;
                    }).handle((_, rollbackFailure) -> rollbackFailure == null
                            ? AdminResult.failure("service.playerstate.god-failed")
                            : AdminResult.failure("service.user.rollback-failed"));
                })).exceptionally(_ -> AdminResult.failure("service.user.persistence-failed"));
    }

    @Override
    public AdminResult heal(CellPlayer player) {
        return platform.heal(player) ? AdminResult.success(
                "service.playerstate.heal-success", Map.of("player", displayNames.plainDisplayName(player))
        ) : AdminResult.failure(
                "service.playerstate.heal-failed", Map.of("player", displayNames.plainDisplayName(player))
        );
    }

    @Override
    public AdminResult feed(CellPlayer player) {
        return platform.feed(player) ? AdminResult.success(
                "service.playerstate.feed-success", Map.of("player", displayNames.plainDisplayName(player))
        ) : AdminResult.failure(
                "service.playerstate.feed-failed", Map.of("player", displayNames.plainDisplayName(player))
        );
    }

    @Override
    public CompletableFuture<AdminResult> setAfk(UUID uuid, String name, boolean afk) {
        var now = System.currentTimeMillis();
        return users.updateVoid(uuid, user -> {
            user.state.afk = afk;
            user.timestamps.lastActivityAt = now;
        }).thenApply(_ -> {
            lastActivityNanos.put(uuid, System.nanoTime());
            return AdminResult.success(
                    afk ? "service.playerstate.afk-enabled" : "service.playerstate.afk-disabled",
                    Map.of("player", name)
            );
        }).exceptionally(_ -> AdminResult.failure("service.user.persistence-failed"));
    }

    @Override
    public boolean afk(UUID uuid) {
        return users.cached(uuid).map(user -> user.state.afk).orElse(false);
    }

    @Override
    public void activity(UUID uuid, long timestamp) {
        lastActivityNanos.put(uuid, System.nanoTime());
        var user = users.cached(uuid).orElse(null);
        if (user == null) return;
        user.timestamps.lastActivityAt = Math.max(user.timestamps.lastActivityAt, timestamp);
        users.markDirty(uuid);
    }

    @Override
    public long lastActivity(UUID uuid) {
        return users.cached(uuid).map(user -> user.timestamps.lastActivityAt).orElse(0L);
    }

    @Override
    public long idleMillis(UUID uuid) {
        var started = lastActivityNanos.get(uuid);
        if (started == null) return -1L;
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }

    @Override
    public CompletableFuture<AdminResult> setNick(UUID uuid, String name, Optional<String> nickname) {
        var online = platform.onlinePlayers().stream().filter(player -> player.uuid().equals(uuid)).findFirst();
        var normalized = nickname.filter(value -> !value.isBlank());
        if (online.isPresent() && normalized.isPresent()) {
            var sanitized = displayNames.sanitizeNickname(online.orElseThrow(), normalized.orElseThrow());
            if (!displayNames.validNickname(online.orElseThrow(), sanitized)) {
                return CompletableFuture.completedFuture(AdminResult.failure("player.nick-invalid"));
            }
            normalized = Optional.of(sanitized);
        }
        var stored = normalized;
        return users.updateVoid(uuid, user -> user.state.nickname = stored.orElse(null))
                .thenCompose(_ -> platform.callOnServerThread(() -> {
                    online.ifPresent(displayNames::refresh);
                    return stored.map(value -> AdminResult.success(
                                    "player.nick-set",
                                    Map.of("nickname", value)
                            ))
                            .orElseGet(() -> AdminResult.success("player.nick-cleared"));
                }))
                .exceptionally(_ -> AdminResult.failure("service.user.persistence-failed"));
    }

    @Override
    public Optional<String> nick(UUID uuid) {
        return users.cached(uuid).flatMap(user -> Optional.ofNullable(user.state.nickname));
    }

    public void forgetActivity(UUID uuid) {
        lastActivityNanos.remove(uuid);
    }

}
