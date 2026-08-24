package top.likoslupus.cellulosesz.core.permission;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public final class DefaultPermissionService implements PermissionService {

    private final Map<PermissionCacheKey, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private PermissionBackend backend = (_, permission) -> permission.isBlank();
    private volatile boolean cacheEnabled = true;
    private volatile long cacheMillis = 5_000L;

    public void backend(PermissionBackend backend) {
        this.backend = requireNonNull(backend, "backend");
        clearCache();
    }

    public void clearCache() {
        cache.clear();
    }

    public void cache(
            boolean enabled,
            long expireSeconds
    ) {
        this.cacheEnabled = enabled;
        this.cacheMillis = Math.max(0L, expireSeconds) * 1_000L;
        clearCache();
    }

    @Override
    public boolean has(CellPlayer player, String permission) {
        requireNonNull(player, "player");
        if (permission.isBlank()) {
            return true;
        }

        return cached(
                player,
                permission,
                "permission",
                () -> backend.has(player, permission)
        );
    }

    @Override
    public int intOption(
            CellPlayer player,
            String key,
            int fallback
    ) {
        return cached(
                player,
                key,
                "int-option",
                () -> backend.intOption(player, key, fallback)
        );
    }

    @Override
    public boolean boolOption(
            CellPlayer player,
            String key,
            boolean fallback
    ) {
        return cached(
                player,
                key,
                "bool-option",
                () -> backend.boolOption(player, key, fallback)
        );
    }

    @Override
    public String stringOption(CellPlayer player, String key) {
        return cached(
                player,
                key,
                "string-option",
                () -> backend.stringOption(player, key)
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(
            CellPlayer player,
            String key,
            String type,
            CacheSupplier<T> supplier
    ) {
        requireNonNull(player, "player");
        if (!cacheEnabled || cacheMillis <= 0L) {
            return supplier.get();
        }

        var cacheKey = new PermissionCacheKey(player.uuid(), key, type);
        var now = System.currentTimeMillis();
        var existing = cache.get(cacheKey);

        if (existing != null && existing.expiresAt >= now) {
            return (T) existing.value;
        }

        var value = supplier.get();
        cache.put(cacheKey, new CacheEntry<>(value, now + cacheMillis));
        return value;
    }

    @FunctionalInterface
    private interface CacheSupplier<T> {

        T get();

    }

    private record CacheEntry<T>(
            T value,
            long expiresAt
    ) {

    }

}
