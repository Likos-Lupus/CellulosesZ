package top.likoslupus.cellulosesz.modules.warp.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;

import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class JsonWarpService implements WarpService, AsyncInitializable {

    private final StorageService storage;
    private final Path warpsDirectory;
    private final WarpConfig config;
    private final ConcurrentHashMap<String, Warp> warps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<Void>> writeTails = new ConcurrentHashMap<>();

    public JsonWarpService(
            StorageService storage,
            Path warpsDirectory,
            WarpConfig config
    ) {
        this.storage = storage;
        this.warpsDirectory = warpsDirectory;
        this.config = config;
    }

    @Override
    public CompletableFuture<List<Warp>> warps() {
        return CompletableFuture.completedFuture(sorted());
    }

    @Override
    public List<Warp> cachedWarps() {
        return sorted();
    }

    @Override
    public CompletableFuture<Optional<Warp>> warp(String name) {
        return CompletableFuture.completedFuture(cachedWarp(name));
    }

    @Override
    public Optional<Warp> cachedWarp(String name) {
        return Optional.ofNullable(warps.get(normalize(name)));
    }

    @Override
    public CompletableFuture<Warp> setWarp(
            String name,
            CellLocation location,
            UUID creator
    ) {
        var key = normalize(name);
        var replacement = new Warp(key, location);
        replacement.createdBy = creator;
        return enqueue(
                key,
                () -> storage.save(path(key), replacement).thenApply(_ -> {
                    warps.put(key, replacement);
                    return replacement;
                })
        );
    }

    @Override
    public CompletableFuture<Boolean> deleteWarp(String name) {
        var key = normalize(name);
        return enqueue(
                key,
                () -> storage.delete(path(key)).thenApply(deleted -> {
                    var removed = warps.remove(key);
                    return deleted || removed != null;
                })
        );
    }

    @Override
    public Optional<String> requiredPermission(Warp warp) {
        if (!config.perWarpPermission) return Optional.empty();
        return Optional.of("cellulosesz.warp." + normalize(warp.name));
    }

    @Override
    public CompletableFuture<Void> reload() {
        return storage.loadDirectory(warpsDirectory, Warp.class)
                .thenAccept(loaded -> {
                    var replacement = new LinkedHashMap<String, Warp>();
                    loaded.forEach(warp -> {
                        validate(warp);
                        replacement.put(normalize(warp.name), warp);
                    });
                    warps.clear();
                    warps.putAll(replacement);
                });
    }

    private <T> CompletableFuture<T> enqueue(String key, Supplier<CompletableFuture<T>> operation) {
        var result = new CompletableFuture<T>();
        writeTails.compute(key, (_, previous) -> {
            var tail = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous;
            var next = tail
                    .handle((_, _) -> null)
                    .thenCompose(_ -> operation.get().thenAccept(result::complete));
            next.whenComplete((_, failure) -> {
                writeTails.remove(key, next);
                if (failure != null) result.completeExceptionally(unwrap(failure));
            });
            return next;
        });
        return result;
    }

    private Path path(String name) {
        return warpsDirectory.resolve(normalize(name) + ".json");
    }

    private Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException completion && completion.getCause() != null
                ? completion.getCause()
                : failure;
    }

    private String normalize(String name) {
        var normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("Warp name must not be blank");
        return normalized;
    }

    private List<Warp> sorted() {
        return warps.values().stream()
                .sorted(Comparator.comparing(warp -> warp.name))
                .toList();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return reload();
    }

    private void validate(Warp warp) {
        if (warp.name.isBlank()) throw new IllegalArgumentException("Warp name must not be blank");
    }

}
