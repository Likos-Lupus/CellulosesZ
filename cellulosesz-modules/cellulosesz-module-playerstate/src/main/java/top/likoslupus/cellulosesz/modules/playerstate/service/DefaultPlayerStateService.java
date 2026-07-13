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

public final class DefaultPlayerStateService implements PlayerStateService {

    private final PlatformService platform;
    private final UserService users;
    private final DisplayNameService displayNames;

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
    public AdminResult setFlying(CellPlayer player, boolean enabled) {
        if (!platform.setFlying(player, enabled)) {
            return AdminResult.failure("service.playerstate.fly-failed");
        }
        users.cached(player.uuid()).ifPresent(user -> {
            user.state.flying = enabled;
            users.markDirty(player.uuid());
        });
        return AdminResult.success(
                enabled ? "service.playerstate.fly-enabled" : "service.playerstate.fly-disabled",
                Map.of("player", displayNames.plainDisplayName(player))
        );
    }

    @Override
    public AdminResult setGod(CellPlayer player, boolean enabled) {
        if (!platform.setInvulnerable(player, enabled)) {
            return AdminResult.failure("service.playerstate.god-failed");
        }
        users.cached(player.uuid()).ifPresent(user -> {
            user.state.god = enabled;
            users.markDirty(player.uuid());
        });
        return AdminResult.success(
                enabled ? "service.playerstate.god-enabled" : "service.playerstate.god-disabled",
                Map.of("player", displayNames.plainDisplayName(player))
        );
    }

    @Override
    public AdminResult heal(CellPlayer player) {
        return platform.heal(player) ? AdminResult.success(
                "service.playerstate.heal-success",
                Map.of("player", displayNames.plainDisplayName(player))
        ) : AdminResult.failure(
                "service.playerstate.heal-failed",
                Map.of("player", displayNames.plainDisplayName(player))
        );
    }

    @Override
    public AdminResult feed(CellPlayer player) {
        return platform.feed(player) ? AdminResult.success(
                "service.playerstate.feed-success",
                Map.of("player", displayNames.plainDisplayName(player))
        ) : AdminResult.failure(
                "service.playerstate.feed-failed",
                Map.of("player", displayNames.plainDisplayName(player))
        );
    }

    @Override
    public AdminResult setAfk(
            UUID uuid,
            String name,
            boolean afk
    ) {
        users.cached(uuid).ifPresent(user -> {
            user.state.afk = afk;
            users.markDirty(uuid);
        });
        return AdminResult.success(
                afk ? "service.playerstate.afk-enabled" : "service.playerstate.afk-disabled",
                Map.of("player", name)
        );
    }

    @Override
    public boolean afk(UUID uuid) {
        return users.cached(uuid)
                .map(user -> user.state.afk)
                .orElse(false);
    }

    @Override
    public AdminResult setNick(
            UUID uuid,
            String name,
            Optional<String> nickname
    ) {
        var online = platform.onlinePlayers().stream()
                .filter(player -> player.uuid().equals(uuid))
                .findFirst();
        var normalized = nickname.filter(value -> !value.isBlank());
        if (online.isPresent() && normalized.isPresent()) {
            var sanitized = displayNames.sanitizeNickname(online.get(), normalized.get());
            if (!displayNames.validNickname(online.get(), sanitized)) {
                return AdminResult.failure("player.nick-invalid");
            }
            normalized = Optional.of(sanitized);
        }

        var stored = normalized;
        var user = users.load(uuid).join();
        var previous = user.state.nickname;
        user.state.nickname = stored.orElse(null);
        users.markDirty(uuid);
        try {
            users.save(uuid).join();
        } catch (RuntimeException _) {
            user.state.nickname = previous;
            users.markDirty(uuid);
            return AdminResult.failure("service.user.persistence-failed");
        }
        online.ifPresent(displayNames::refresh);

        return stored
                .map(value -> AdminResult.success(
                        "player.nick-set",
                        Map.of("nickname", value)
                ))
                .orElseGet(() -> AdminResult.success("player.nick-cleared"));
    }

    @Override
    public Optional<String> nick(UUID uuid) {
        return users.cached(uuid)
                .flatMap(user -> Optional.ofNullable(user.state.nickname));
    }

}
