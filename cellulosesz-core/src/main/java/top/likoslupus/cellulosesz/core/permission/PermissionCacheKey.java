package top.likoslupus.cellulosesz.core.permission;

import java.util.UUID;

record PermissionCacheKey(
        UUID playerId,
        String key,
        String type
) {

}
