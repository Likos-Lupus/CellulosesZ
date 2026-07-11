package top.likoslupus.cellulosesz.modules.playerstate.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.UUID;

public final class DefaultVanishService implements VanishService {

    private final PlatformService platform;
    private final UserService users;
    private final PermissionService permissions;

    public DefaultVanishService(
            PlatformService platform,
            UserService users,
            PermissionService permissions
    ) {
        this.platform = platform;
        this.users = users;
        this.permissions = permissions;
    }

    @Override
    public boolean vanished(UUID uuid) {
        return users.cached(uuid)
                .map(user -> user.state.vanished)
                .orElse(false);
    }

    @Override
    public AdminResult setVanished(CellPlayer player, boolean vanished) {
        var user = users.cached(player.uuid());
        if (user.isEmpty()) return AdminResult.failure("玩家数据尚未加载: " + player.name());

        user.get().state.vanished = vanished;
        users.markDirty(player.uuid());
        platform.setVanishedState(player, vanished);
        platform.onlinePlayers().stream()
                .filter(viewer -> !viewer.uuid().equals(player.uuid()))
                .forEach(viewer -> {
                    if (vanished) {
                        if (!canSee(viewer, player.uuid())) {
                            platform.setPlayerVisible(viewer, player, false);
                        }
                    } else {
                        platform.setPlayerVisible(viewer, player, true);
                    }
                });
        return AdminResult.success((vanished ? "已隐身: " : "已显身: ") + player.name());
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
                .filter(target -> !viewer.uuid().equals(target.uuid())
                        && !canSee(viewer, target.uuid())
                )
                .forEach(target -> platform.setPlayerVisible(
                        viewer,
                        target,
                        false
                ));
    }

}
