package top.likoslupus.cellulosesz.modules.playerstate.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.playerstate.VanishPlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class DefaultVanishService implements VanishService {

    private final VanishPlatformService platform;
    private final PlayerDirectory players;
    private final ServerThreadExecutor serverThread;
    private final UserService users;
    private final PermissionService permissions;
    private final DisplayNameService displayNames;

    public DefaultVanishService(
            VanishPlatformService platform,
            PlayerDirectory players,
            ServerThreadExecutor serverThread,
            UserService users,
            PermissionService permissions,
            DisplayNameService displayNames
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.players = requireNonNull(players, "players");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.users = requireNonNull(users, "users");
        this.permissions = requireNonNull(permissions, "permissions");
        this.displayNames = requireNonNull(displayNames, "displayNames");
    }

    public void clear(CellPlayer player) {
        platform.setVanishedState(player, false);
    }

    @Override
    public boolean vanished(UUID uuid) {
        return users
                .cached(uuid)
                .map(user -> user.state().vanished())
                .orElse(false);
    }

    @Override
    public CompletableFuture<AdminResult> setVanished(CellPlayer player, boolean vanished) {
        var previous = vanished(player.uuid());
        return serverThread
                .submit(() -> applyPlatform(player, vanished))
                .thenCompose(applied -> {
                    if (!applied) {
                        return serverThread
                                .submit(() -> applyPlatform(player, previous))
                                .thenApply(rolledBack ->
                                        rolledBack
                                                ? AdminResult.failure("service.playerstate.vanish-failed")
                                                : AdminResult.failure("service.user.rollback-failed")
                                );
                    }

                    return users
                            .updateVoid(
                                    player.uuid(),
                                    user -> user.withState(user.state().withVanished(vanished))
                            )
                            .thenApply(_ -> AdminResult.success(
                                    vanished
                                            ? "service.playerstate.vanish-enabled"
                                            : "service.playerstate.vanish-disabled",
                                    Map.of("player", displayNames.plainDisplayName(player))
                            ))
                            .exceptionallyCompose(_ -> serverThread
                                    .submit(() -> applyPlatform(player, previous))
                                    .thenApply(rolledBack ->
                                            rolledBack
                                                    ? AdminResult.failure("service.user.persistence-failed")
                                                    : AdminResult.failure("service.user.rollback-failed")
                                    )
                            );
                });
    }

    private boolean applyPlatform(CellPlayer player, boolean vanished) {
        var state = platform.setVanishedState(player, vanished);
        if (!state.successful()) return false;

        var successful = true;
        for (var viewer : players.onlinePlayers()) {
            if (viewer.uuid().equals(player.uuid())) {
                continue;
            }

            var visible = !vanished || canSee(viewer, player.uuid());
            if (!platform.setVisible(viewer, player, visible).successful()) {
                successful = false;
            }
        }

        return successful;
    }

    @Override
    public boolean canSee(CellPlayer viewer, UUID target) {
        return viewer.uuid().equals(target)
                || !vanished(target)
                || permissions.has(viewer, "cellulosesz.playerstate.vanish.see");
    }

    @Override
    public void synchronizeViewer(CellPlayer viewer) {
        players.onlinePlayers().stream()
                .filter(target -> !viewer.uuid().equals(target.uuid()))
                .forEach(target -> platform.setVisible(
                        viewer,
                        target,
                        canSee(viewer, target.uuid())
                ));
    }

}
