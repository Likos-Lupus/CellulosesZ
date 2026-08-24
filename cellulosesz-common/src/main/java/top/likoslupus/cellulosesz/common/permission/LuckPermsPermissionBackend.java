package top.likoslupus.cellulosesz.common.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;


import static java.util.Objects.requireNonNull;

/** Direct, optional LuckPerms API adapter using stable player UUIDs. */
public final class LuckPermsPermissionBackend implements PermissionBackend {

    private final LuckPerms luckPerms;

    public LuckPermsPermissionBackend(LuckPerms luckPerms) {
        this.luckPerms = requireNonNull(luckPerms, "luckPerms");
    }

    public static LuckPermsPermissionBackend fromProvider() {
        return new LuckPermsPermissionBackend(LuckPermsProvider.get());
    }

    @Override
    public boolean has(CellPlayer player, String permission) {
        return permission.isBlank() || user(player)
                .getCachedData()
                .getPermissionData()
                .checkPermission(permission)
                .asBoolean();
    }

    @Override
    public String stringOption(CellPlayer player, String key) {
        return user(player).getCachedData().getMetaData().getMetaValue(key);
    }

    private User user(CellPlayer player) {
        var user = luckPerms.getUserManager().getUser(player.uuid());
        if (user == null) {
            throw new IllegalStateException(
                    "LuckPerms user is not loaded: " + player.uuid()
            );
        }
        return user;
    }

}
