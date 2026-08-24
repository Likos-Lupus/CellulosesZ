package top.likoslupus.cellulosesz.modules.playerstate.service;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateResult;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.playerstate.VanishPlatformService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;

import java.util.ArrayList;
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
        var user = users.cached(uuid);
        return user != null && user.state().vanished();
    }

    @Override
    public CompletableFuture<PlayerStateResult> setVanished(CellPlayer player, boolean vanished) {
        var previous = vanished(player.uuid());
        return serverThread
                .submit(() -> applyPlatform(player, vanished))
                .thenCompose(applied -> {
                    if (!applied.successful()) {
                        return serverThread
                                .submit(() -> applyPlatform(player, previous))
                                .thenApply(rolledBack -> rolledBack.successful()
                                        ?
                                        PlayerStateResult.failure(
                                                "service.playerstate.vanish-failed",
                                                MessageArguments.empty()
                                        )
                                        : PlayerStateResult.failure(
                                                "service.user.rollback-failed",
                                                MessageArguments.empty()
                                        )
                                );
                    }

                    return users
                            .updateVoid(
                                    player.uuid(),
                                    user -> user.withState(user.state().withVanished(vanished))
                            )
                            .thenApply(_ -> PlayerStateResult.success(
                                    vanished
                                            ? "service.playerstate.vanish-enabled"
                                            : "service.playerstate.vanish-disabled",
                                    MessageArguments.builder()
                                            .add(displayNames.plainDisplayName(player))
                                            .build()
                            ))
                            .exceptionallyCompose(_ -> serverThread
                                    .submit(() -> applyPlatform(player, previous))
                                    .thenApply(rolledBack -> rolledBack.successful()
                                            ?
                                            PlayerStateResult.failure(
                                                    "service.user.persistence-failed",
                                                    MessageArguments.empty()
                                            )
                                            : PlayerStateResult.failure(
                                                    "service.user.rollback-failed",
                                                    MessageArguments.empty()
                                            )
                                    )
                            );
                });
    }

    private PlatformResult<Void> applyPlatform(CellPlayer player, boolean vanished) {
        var state = platform.setVanishedState(player, vanished);
        if (!state.successful()) {
            return PlatformResult.failure(state.status(), state.detail());
        }

        var failures = new ArrayList<String>();
        PlatformOperationStatus failureStatus = null;
        for (var viewer : players.onlinePlayers()) {
            if (viewer.uuid().equals(player.uuid())) {
                continue;
            }

            var visible = !vanished || canSee(viewer, player.uuid());
            var visibility = platform.setVisible(viewer, player, visible);
            if (!visibility.successful()) {
                if (failureStatus == null) {
                    failureStatus = visibility.status();
                }
                failures.add(viewer.uuid() + ": " + visibility.detail());
            }
        }

        return failures.isEmpty()
                ? PlatformResult.success()
                : PlatformResult.failure(
                        failureStatus,
                        "Visibility updates failed: " + String.join("; ", failures)
                );
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
