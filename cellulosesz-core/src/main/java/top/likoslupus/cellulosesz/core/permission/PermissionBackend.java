package top.likoslupus.cellulosesz.core.permission;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;

public interface PermissionBackend {

    boolean has(
            CellPlayer player,
            String permission
    );

    default int intOption(
            CellPlayer player,
            String key,
            int fallback
    ) {
        return fallback;
    }

    default boolean boolOption(
            CellPlayer player,
            String key,
            boolean fallback
    ) {
        return fallback;
    }

    default Optional<String> stringOption(
            CellPlayer player,
            String key
    ) {
        return Optional.empty();
    }

}
