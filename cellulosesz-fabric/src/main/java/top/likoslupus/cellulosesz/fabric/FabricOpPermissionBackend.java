package top.likoslupus.cellulosesz.fabric;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;

public final class FabricOpPermissionBackend implements PermissionBackend {

    private final int opLevel;

    public FabricOpPermissionBackend(int opLevel) {
        this.opLevel = opLevel;
    }

    @Override
    public boolean has(Object source, String permission) {
        if (permission.isBlank()) return true;
        if (source instanceof CommandSourceStack commandSource) {
            return commandSource.permissions().hasPermission(
                    new Permission.HasCommandLevel(PermissionLevel.byId(opLevel))
            );
        }
        return false;
    }

}
