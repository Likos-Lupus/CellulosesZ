package top.likoslupus.cellulosesz.modules.user.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DefaultPlayerResolver implements PlayerResolver {

    private static final String SEE_VANISHED = "cellulosesz.playerstate.vanish.see";

    private final PlatformService platform;
    private final UserService users;
    private final NameCacheService names;
    private final PermissionService permissions;
    private final DisplayNameService displayNames;

    public DefaultPlayerResolver(
            PlatformService platform,
            UserService users,
            NameCacheService names,
            PermissionService permissions,
            DisplayNameService displayNames
    ) {
        this.platform = platform;
        this.users = users;
        this.names = names;
        this.permissions = permissions;
        this.displayNames = displayNames;
    }

    @Override
    public ResolvedPlayer resolveKnown(String input, @Nullable CellPlayer viewer) {
        if (input.isBlank()) return unknown(input);

        var online = platform.onlinePlayer(input);
        if (online.isPresent()) return visible(wrapOnline(online.get()), viewer);

        var displayMatches = platform.onlinePlayers().stream()
                .filter(player -> displayNames.plainDisplayName(player).equalsIgnoreCase(input))
                .toList();
        if (displayMatches.size() == 1) return visible(wrapOnline(displayMatches.getFirst()), viewer);
        if (displayMatches.size() > 1) return unknown(input);

        try {
            return resolveKnown(UUID.fromString(input), viewer);
        } catch (IllegalArgumentException _) {
            var uuid = names.findUuid(input);
            if (uuid.isPresent()) return resolveKnown(uuid.get(), viewer);

            var cachedMatches = users.cachedUsers().stream()
                    .filter(user -> user.lastKnownName != null)
                    .filter(user -> displayNames.displayName(user.uuid, user.lastKnownName)
                            .plainText().equalsIgnoreCase(input)
                    )
                    .toList();
            return cachedMatches.size() == 1
                    ? resolveKnown(cachedMatches.getFirst().uuid, viewer)
                    : unknown(input);
        }
    }

    @Override
    public ResolvedPlayer resolveKnown(UUID uuid, @Nullable CellPlayer viewer) {
        var online = platform.onlinePlayers().stream()
                .filter(player -> player.uuid().equals(uuid))
                .findFirst();
        if (online.isPresent()) return visible(wrapOnline(online.get()), viewer);

        var cached = users.cached(uuid);
        var name = names.findName(uuid)
                .or(() ->
                        cached.flatMap(user -> Optional.ofNullable(user.lastKnownName))
                                .filter(value -> !value.isBlank())
                );
        if (name.isEmpty()) return unknown(uuid.toString());

        var vanished = cached
                .map(user -> user.state.vanished)
                .orElse(false);
        return visible(new ResolvedPlayer(
                ResolvedPlayerState.OFFLINE,
                uuid,
                name.get(),
                null,
                vanished
        ), viewer);
    }

    @Override
    public CompletableFuture<ResolvedPlayer> resolve(String input, @Nullable CellPlayer viewer) {
        var known = resolveKnown(input, viewer);
        if (known.state() != ResolvedPlayerState.OFFLINE || known.uuid() == null) {
            return CompletableFuture.completedFuture(known);
        }

        return users.load(known.uuid()).thenApply(user ->
                visible(new ResolvedPlayer(
                        ResolvedPlayerState.OFFLINE,
                        user.uuid,
                        user.lastKnownName == null ? known.name() : user.lastKnownName,
                        null,
                        user.state.vanished
                ), viewer)
        );
    }

    private ResolvedPlayer wrapOnline(CellPlayer player) {
        var vanished = users.cached(player.uuid())
                .map(user -> user.state.vanished)
                .orElse(false);
        return new ResolvedPlayer(
                ResolvedPlayerState.ONLINE,
                player.uuid(),
                player.name(),
                player,
                vanished
        );
    }

    private ResolvedPlayer visible(ResolvedPlayer resolved, @Nullable CellPlayer viewer) {
        if (!resolved.vanished() || viewer == null) return resolved;
        if (viewer.uuid().equals(resolved.uuid())
                || permissions.has(viewer.nativeHandle(), SEE_VANISHED)
        ) return resolved;
        return unknown(resolved.name());
    }

    private ResolvedPlayer unknown(String input) {
        return new ResolvedPlayer(
                ResolvedPlayerState.UNKNOWN,
                null,
                input,
                null,
                false
        );
    }

}
