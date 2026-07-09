package top.likoslupus.cellulosesz.core.permission;

import top.likoslupus.cellulosesz.api.permission.PermissionService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultPermissionService implements PermissionService {

    private final Map<PermissionCacheKey, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private PermissionBackend backend = (_, permission) -> permission.isBlank();
    private volatile boolean cacheEnabled = true;
    private volatile long cacheMillis = 5_000L;

    public void backend(PermissionBackend backend) {
        this.backend = backend;
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
    public boolean has(
            Object source,
            String permission
    ) {
        if (permission.isBlank()) return true;
        return cached(source, permission, "permission", () -> backend.has(source, permission));
    }

    @Override
    public int intOption(
            Object source,
            String key,
            int fallback
    ) {
        return cached(source, key, "int-option", () -> backend.intOption(source, key, fallback));
    }

    @Override
    public boolean boolOption(
            Object source,
            String key,
            boolean fallback
    ) {
        return cached(source, key, "bool-option", () -> backend.boolOption(source, key, fallback));
    }

    @Override
    public Optional<String> stringOption(
            Object source,
            String key
    ) {
        return cached(source, key, "string-option", () -> backend.stringOption(source, key));
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(
            Object source,
            String key,
            String type,
            CacheSupplier<T> supplier
    ) {
        if (!cacheEnabled || cacheMillis <= 0L) return supplier.get();

        var cacheKey = new PermissionCacheKey(System.identityHashCode(source), key, type);
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
