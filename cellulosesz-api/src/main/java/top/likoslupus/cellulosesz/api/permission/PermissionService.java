package top.likoslupus.cellulosesz.api.permission;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;

/**
 * Permission queries for a stable player identity. Backend failures are propagated, not represented
 * as absence.
 */
public interface PermissionService {

    boolean has(
            CellPlayer player,
            String permission
    );

    int intOption(
            CellPlayer player,
            String key,
            int fallback
    );

    boolean boolOption(
            CellPlayer player,
            String key,
            boolean fallback
    );

    Optional<String> stringOption(
            CellPlayer player,
            String key
    );

}
