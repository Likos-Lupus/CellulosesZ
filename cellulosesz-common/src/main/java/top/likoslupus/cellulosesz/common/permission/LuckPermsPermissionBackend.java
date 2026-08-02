package top.likoslupus.cellulosesz.common.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/** Direct, optional LuckPerms API adapter. */
public final class LuckPermsPermissionBackend implements PermissionBackend {

    private final LuckPerms luckPerms;

    public LuckPermsPermissionBackend(LuckPerms luckPerms) {
        this.luckPerms = requireNonNull(luckPerms, "luckPerms");
    }

    public static LuckPermsPermissionBackend fromProvider() {
        return new LuckPermsPermissionBackend(LuckPermsProvider.get());
    }

    @Override
    public boolean has(Object source, String permission) {
        if (permission.isBlank()) {
            return true;
        }
        return user(source)
                .map(value -> value.getCachedData()
                        .getPermissionData()
                        .checkPermission(permission)
                        .asBoolean())
                .orElse(false);
    }

    @Override
    public Optional<String> stringOption(Object source, String key) {
        return user(source)
                .map(User::getCachedData)
                .map(cached -> cached.getMetaData().getMetaValue(key));
    }

    private Optional<User> user(Object source) {
        return uuid(source).map(luckPerms.getUserManager()::getUser);
    }

    private static Optional<UUID> uuid(Object source) {
        return switch (source) {
            case UUID uuid -> Optional.of(uuid);
            case CellPlayer player -> Optional.of(player.uuid());
            case ServerPlayer player -> Optional.of(player.getUUID());
            case CommandSourceStack stack when stack.getEntity() instanceof ServerPlayer player ->
                    Optional.of(player.getUUID());
            default -> Optional.empty();
        };
    }

}
