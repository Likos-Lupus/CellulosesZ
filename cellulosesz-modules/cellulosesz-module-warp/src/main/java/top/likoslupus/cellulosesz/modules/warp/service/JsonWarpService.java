package top.likoslupus.cellulosesz.modules.warp.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.module.PreparedModuleReload;
import top.likoslupus.cellulosesz.api.module.PreparedReloads;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.warp.PreparedWarpReload;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.core.concurrent.KeyedSerialAsyncQueue;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class JsonWarpService implements WarpService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_PER_WARP = 1_024;
    private static final int MAXIMUM_PENDING_RELOADS = 32;

    private final StorageService storage;
    private final Path warpsDirectory;
    private final KeyedSerialAsyncQueue<String> mutations = new KeyedSerialAsyncQueue<>(
            Runnable::run,
            MAXIMUM_PENDING_PER_WARP
    );
    private final SerialAsyncQueue reloads = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_RELOADS
    );
    private RuntimeState state;
    private long mutationVersion;

    public JsonWarpService(
            StorageService storage,
            Path warpsDirectory,
            WarpConfig config
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.warpsDirectory = requireNonNull(warpsDirectory, "warpsDirectory");
        this.state = new RuntimeState(Map.of(), requireNonNull(config, "config").perWarpPermission);
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
    public synchronized Optional<Warp> cachedWarp(String name) {
        return Optional.ofNullable(state.warps().get(normalize(name)))
                .map(this::copyWarp);
    }

    @Override
    public CompletableFuture<Warp> setWarp(String name, CellLocation location, UUID creator) {
        var key = normalize(name);
        var replacement = new Warp(key, copyLocation(requireNonNull(location, "location")));
        replacement.createdBy = creator;
        var candidate = validatedCopy(replacement);

        return enqueue(
                key,
                () -> storage
                        .save(path(key), candidate)
                        .thenApply(_ -> {
                            synchronized (this) {
                                var next = new LinkedHashMap<>(state.warps());
                                next.put(key, candidate);
                                state = new RuntimeState(
                                        Map.copyOf(next),
                                        state.perWarpPermission()
                                );
                                mutationVersion++;
                            }

                            return copyWarp(candidate);
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
                            synchronized (this) {
                                if (!state.warps().containsKey(key)) {
                                    return deleted;
                                }

                                var next = new LinkedHashMap<>(state.warps());
                                next.remove(key);
                                state = new RuntimeState(
                                        Map.copyOf(next),
                                        state.perWarpPermission()
                                );
                                mutationVersion++;
                                return true;
                            }
                        })
        );
    }

    @Override
    public synchronized Optional<String> requiredPermission(Warp warp) {
        if (!state.perWarpPermission()) {
            return Optional.empty();
        }

        return Optional.of("cellulosesz.warp." + normalize(warp.name));
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

    private synchronized List<Warp> sorted() {
        return state.warps().values().stream()
                .sorted(Comparator.comparing(warp -> warp.name))
                .map(this::copyWarp)
                .toList();
    }

    private Warp copyWarp(Warp source) {
        var copy = new Warp();
        copy.name = source.name;
        copy.displayName = source.displayName;
        copy.cost = source.cost;
        copy.location = copyLocation(source.location);
        copy.createdBy = source.createdBy;
        copy.createdAt = source.createdAt;
        return copy;
    }

    private CellLocation copyLocation(CellLocation source) {
        return new CellLocation(
                source.world,
                source.x, source.y, source.z,
                source.yaw, source.pitch
        );
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return prepareReload(state.perWarpPermission())
                .thenCompose(prepared -> prepared.commit().toCompletableFuture());
    }

    public CompletableFuture<PreparedWarpReload> prepareReload(boolean perWarpPermission) {
        return reloads.submit(() -> {
            final RuntimeState previous;
            final long preparedVersion;
            synchronized (this) {
                previous = state;
                preparedVersion = mutationVersion;
            }

            return storage
                    .loadDirectory(warpsDirectory, Warp.class)
                    .thenApply(loaded -> {
                        var next = new LinkedHashMap<String, Warp>();
                        loaded.stream()
                                .map(this::validatedCopy)
                                .sorted(Comparator.comparing(warp -> warp.name))
                                .forEach(warp -> {
                                    var key = normalize(warp.name);
                                    if (next.putIfAbsent(key, warp) != null) {
                                        throw new IllegalStateException(
                                                "Duplicate warp name: " + key);
                                    }
                                });

                        return new PreparedWarpReloadImpl(
                                previous,
                                new RuntimeState(Map.copyOf(next), perWarpPermission),
                                preparedVersion
                        );
                    });
        });
    }

    private Warp validatedCopy(Warp source) {
        requireNonNull(source, "warp");
        var copy = copyWarp(source);
        copy.name = normalize(copy.name);
        copy.displayName = requireNonNull(copy.displayName, "warp.displayName");
        copy.cost = requireNonNull(copy.cost, "warp.cost");
        copy.location = copyLocation(requireNonNull(copy.location, "warp.location"));
        return copy;
    }

    @Override
    public void stopAccepting() {
        reloads.stopAccepting();
        mutations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return CompletableFuture.allOf(reloads.drain(), mutations.drain());
    }

    private record RuntimeState(
            Map<String, Warp> warps,
            boolean perWarpPermission
    ) {

    }

    private final class PreparedWarpReloadImpl implements PreparedWarpReload {

        private final RuntimeState previous;
        private final RuntimeState candidate;
        private final long preparedVersion;
        private final PreparedModuleReload delegate;
        private boolean committed;
        private long committedVersion;

        private PreparedWarpReloadImpl(
                RuntimeState previous,
                RuntimeState candidate,
                long preparedVersion
        ) {
            this.previous = previous;
            this.candidate = candidate;
            this.preparedVersion = preparedVersion;
            this.delegate = PreparedReloads.of(this::commitInternal, this::rollbackInternal);
        }

        private CompletionStage<Void> commitInternal() {
            synchronized (JsonWarpService.this) {
                if (mutationVersion != preparedVersion) {
                    return CompletableFuture.failedFuture(new IllegalStateException(
                            "Warp reload became stale before commit"
                    ));
                }

                state = candidate;
                mutationVersion++;
                committedVersion = mutationVersion;
                committed = true;
            }

            return CompletableFuture.completedFuture(null);
        }

        private CompletionStage<Void> rollbackInternal() {
            synchronized (JsonWarpService.this) {
                if (!committed) {
                    return CompletableFuture.completedFuture(null);
                }

                if (mutationVersion != committedVersion) {
                    return CompletableFuture.failedFuture(new IllegalStateException(
                            "Warp reload cannot overwrite a newer successful mutation"
                    ));
                }

                state = previous;
                mutationVersion++;
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> commit() {
            return delegate.commit();
        }

        @Override
        public CompletionStage<Void> rollback() {
            return delegate.rollback();
        }

    }

}
