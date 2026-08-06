package top.likoslupus.cellulosesz.common.permission;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;

import static java.util.Objects.requireNonNull;

public final class MinecraftOpPermissionBackend implements PermissionBackend {

    private final MinecraftServerHandle server;
    private final int opLevel;

    public MinecraftOpPermissionBackend(
            MinecraftServerHandle server,
            int opLevel
    ) {
        this.server = requireNonNull(server, "server");
        this.opLevel = opLevel;
    }

    @Override
    public boolean has(CellPlayer player, String permission) {
        if (permission.isBlank()) {
            return true;
        }

        var current = server.requireRunning();
        var identity = requireNonNull(player, "player");
        var entry = current.getPlayerList()
                .getOps()
                .get(new NameAndId(
                        identity.uuid(),
                        identity.name()
                ));

        return entry != null
                && entry.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(opLevel))
        );
    }

}
