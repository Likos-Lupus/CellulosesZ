package top.likoslupus.cellulosesz.api.permission;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;

public interface PermissionService {

    default boolean has(CellPlayer player, String permission) {
        return has(player.nativeHandle(), permission);
    }

    boolean has(Object source, String permission);

    default int intOption(
            CellPlayer player,
            String key,
            int fallback
    ) {
        return intOption(player.nativeHandle(), key, fallback);
    }

    int intOption(
            Object source,
            String key,
            int fallback
    );

    default boolean boolOption(
            CellPlayer player,
            String key,
            boolean fallback
    ) {
        return boolOption(player.nativeHandle(), key, fallback);
    }

    boolean boolOption(
            Object source,
            String key,
            boolean fallback
    );

    default Optional<String> stringOption(CellPlayer player, String key) {
        return stringOption(player.nativeHandle(), key);
    }

    Optional<String> stringOption(Object source, String key);

}
