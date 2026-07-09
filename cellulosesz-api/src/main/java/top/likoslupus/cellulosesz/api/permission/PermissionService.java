package top.likoslupus.cellulosesz.api.permission;

import java.util.Optional;

public interface PermissionService {

    boolean has(
            Object source,
            String permission
    );

    int intOption(
            Object source,
            String key,
            int fallback
    );

    boolean boolOption(
            Object source,
            String key,
            boolean fallback
    );

    Optional<String> stringOption(
            Object source,
            String key
    );

}
