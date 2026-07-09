package top.likoslupus.cellulosesz.core.permission;

import java.util.Optional;

public interface PermissionBackend {

    boolean has(
            Object source,
            String permission
    );

    default int intOption(
            Object source,
            String key,
            int fallback
    ) {
        return fallback;
    }

    default boolean boolOption(
            Object source,
            String key,
            boolean fallback
    ) {
        return fallback;
    }

    default Optional<String> stringOption(
            Object source,
            String key
    ) {
        return Optional.empty();
    }

}
