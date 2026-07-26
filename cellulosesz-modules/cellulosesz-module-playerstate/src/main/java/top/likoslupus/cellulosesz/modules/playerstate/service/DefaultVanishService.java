package top.likoslupus.cellulosesz.modules.playerstate.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DefaultVanishService implements VanishService {

    private final PlatformService platform;
    private final UserService users;
    private final PermissionService permissions;
    private final DisplayNameService displayNames;

    public DefaultVanishService(
            PlatformService platform,
            UserService users,
            PermissionService permissions,
            DisplayNameService displayNames
    ) {
        this.platform = platform;
        this.users = users;
        this.permissions = permissions;
        this.displayNames = displayNames;
    }

    private static final class PlatformVanishException extends RuntimeException {

        private final boolean previous;

        private PlatformVanishException(boolean previous, Throwable cause) {
            super(cause);
            this.previous = previous;
        }

    }    @Override
    public boolean vanished(UUID uuid) {
        return users.cached(uuid).map(user -> user.state.vanished).orElse(false);
    }

    @Override
    public CompletableFuture<AdminResult> setVanished(CellPlayer player, boolean vanished) {
        return users.update(player.uuid(), user -> {
            var previous = user.state.vanished;
            user.state.vanished = vanished;
            return previous;
        }).thenCompose(previous -> platform.callOnServerThread(() -> {
            try {
                platform.setVanishedState(player, vanished);
                platform.onlinePlayers().stream()
                        .filter(viewer -> !viewer.uuid().equals(player.uuid()))
                        .forEach(viewer -> {
                            if (vanished && !canSee(viewer, player.uuid())) {
                                platform.setPlayerVisible(viewer, player, false);
                            } else if (!vanished) {
                                platform.setPlayerVisible(viewer, player, true);
                            }
                        });
                return AdminResult.success(
                        vanished ? "service.playerstate.vanish-enabled" : "service.playerstate.vanish-disabled",
                        Map.of("player", displayNames.plainDisplayName(player))
                );
            } catch (RuntimeException failure) {
                throw new PlatformVanishException(previous, failure);
            }
        }).exceptionallyCompose(failure -> {
            var cause = unwrap(failure);
            if (!(cause instanceof PlatformVanishException platformFailure)) {
                return CompletableFuture.completedFuture(AdminResult.failure("service.user.persistence-failed"));
            }
            return users.updateVoid(player.uuid(), user -> {
                if (user.state.vanished == vanished) user.state.vanished = platformFailure.previous;
            }).handle((_, rollbackFailure) -> rollbackFailure == null
                    ? AdminResult.failure("service.playerstate.vanish-failed")
                    : AdminResult.failure("service.user.rollback-failed"));
        }));
    }

    @Override
    public boolean canSee(CellPlayer viewer, UUID target) {
        if (viewer.uuid().equals(target)) return true;
        return !vanished(target)
                || permissions.has(viewer.nativeHandle(), "cellulosesz.playerstate.vanish.see");
    }

    @Override
    public void synchronizeViewer(CellPlayer viewer) {
        platform.onlinePlayers().stream()
                .filter(target -> !viewer.uuid().equals(target.uuid()) && !canSee(viewer, target.uuid()))
                .forEach(target -> platform.setPlayerVisible(viewer, target, false));
    }

    private Throwable unwrap(Throwable failure) {
        return failure instanceof java.util.concurrent.CompletionException completion
                && completion.getCause() != null ? completion.getCause() : failure;
    }



}
