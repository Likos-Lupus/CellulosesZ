package top.likoslupus.cellulosesz.modules.warp.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.core.concurrent.KeyedSerialAsyncQueue;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class JsonWarpService implements WarpService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_PER_WARP = 1_024;
    private static final int MAXIMUM_PENDING_RELOADS = 32;

    private final StorageService storage;
    private final Path warpsDirectory;
    private final WarpConfig config;
    private final ConcurrentHashMap<String, Warp> warps = new ConcurrentHashMap<>();
    private final KeyedSerialAsyncQueue<String> mutations = new KeyedSerialAsyncQueue<>(
            Runnable::run,
            MAXIMUM_PENDING_PER_WARP
    );
    private final SerialAsyncQueue reloads = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_RELOADS
    );

    public JsonWarpService(
            StorageService storage,
            Path warpsDirectory,
            WarpConfig config
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.warpsDirectory = requireNonNull(warpsDirectory, "warpsDirectory");
        this.config = requireNonNull(config, "config");
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
    public CompletableFuture<Warp> setWarp(String name, CellLocation location, UUID creator) {
        var key = normalize(name);
        requireNonNull(location, "location");

        var replacement = new Warp(key, location);
        replacement.createdBy = creator;

        return enqueue(
                key,
                () -> storage
                        .save(path(key), replacement)
                        .thenApply(_ -> {
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
                () -> storage
                        .delete(path(key))
                        .thenApply(deleted -> {
                            var removed = warps.remove(key);
                            return deleted || removed != null;
                        })
        );
    }

    @Override
    public Optional<String> requiredPermission(Warp warp) {
        if (!config.perWarpPermission) {
            return Optional.empty();
        }

        return Optional.of("cellulosesz.warp." + normalize(warp.name));
    }

    @Override
    public CompletableFuture<Void> reload() {
        return reloads.submit(() -> storage
                .loadDirectory(warpsDirectory, Warp.class)
                .thenAccept(loaded -> {
                    var replacement = new LinkedHashMap<String, Warp>();
                    loaded.forEach(warp -> {
                        validate(warp);
                        replacement.put(normalize(warp.name), warp);
                    });

                    warps.clear();
                    warps.putAll(replacement);
                }));
    }

    private <T> CompletableFuture<T> enqueue(
            String key,
            Supplier<? extends CompletableFuture<T>> operation
    ) {
        return mutations.submit(key, operation);
    }

    private Path path(String name) {
        return warpsDirectory.resolve(normalize(name) + ".json");
    }

    private String normalize(String name) {
        var normalized = requireNonNull(name, "name").trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Warp name must not be blank");
        }

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
        if (warp.name.isBlank()) {
            throw new IllegalArgumentException("Warp name must not be blank");
        }
    }

    @Override
    public void stopAccepting() {
        reloads.stopAccepting();
        mutations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return CompletableFuture.allOf(
                reloads.drain(),
                mutations.drain()
        );
    }

}
