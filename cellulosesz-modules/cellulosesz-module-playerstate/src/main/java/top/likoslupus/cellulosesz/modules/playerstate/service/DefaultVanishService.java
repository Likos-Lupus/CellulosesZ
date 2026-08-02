package top.likoslupus.cellulosesz.modules.playerstate.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.playerstate.VanishPlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.user.UserService;

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
                    if (!applied.successful()) {
                        return serverThread
                                .submit(() -> applyPlatform(player, previous))
                                .thenApply(rolledBack -> rolledBack.successful()
                                        ?
                                        AdminResult.failure(
                                                "service.playerstate.vanish-failed",
                                                MessageArguments.of("detail", applied.detail())
                                        )
                                        : AdminResult.failure(
                                                "service.user.rollback-failed",
                                                MessageArguments.builder()
                                                        .put("detail", applied.detail())
                                                        .put("rollback", rolledBack.detail())
                                                        .build()
                                        )
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
                                    MessageArguments.builder()
                                            .put("player", displayNames.plainDisplayName(player))
                                            .build()
                            ))
                            .exceptionallyCompose(failure -> serverThread
                                    .submit(() -> applyPlatform(player, previous))
                                    .thenApply(rolledBack -> rolledBack.successful()
                                            ?
                                            AdminResult.failure(
                                                    "service.user.persistence-failed",
                                                    MessageArguments.of(
                                                            "detail",
                                                            failure.getMessage() == null
                                                                    ? failure.getClass()
                                                                    .getSimpleName()
                                                                    : failure.getMessage()
                                                    )
                                            )
                                            : AdminResult.failure(
                                                    "service.user.rollback-failed",
                                                    MessageArguments.of(
                                                            "detail",
                                                            rolledBack.detail()
                                                    )
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
                        failureStatus == null
                                ? PlatformOperationStatus.INTERNAL_ERROR
                                : failureStatus,
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
