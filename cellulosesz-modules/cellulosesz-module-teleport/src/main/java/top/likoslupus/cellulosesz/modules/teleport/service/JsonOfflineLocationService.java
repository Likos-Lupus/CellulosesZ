package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.OfflineLocationService;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.teleport.data.OfflineLocationDocument;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class JsonOfflineLocationService
        implements OfflineLocationService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_MUTATIONS = 4_096;

    private final StorageService storage;
    private final Path path;
    private final SerialAsyncQueue mutations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_MUTATIONS
    );
    private OfflineLocationDocument document = new OfflineLocationDocument();

    public JsonOfflineLocationService(StorageService storage, Path path) {
        this.storage = requireNonNull(storage, "storage");
        this.path = requireNonNull(path, "path");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage
                .createIfMissing(
                        path,
                        OfflineLocationDocument.class,
                        OfflineLocationDocument::new
                )
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = loaded;
                    }
                });
    }

    @Override
    public CompletableFuture<Void> remember(UUID uuid, CellLocation location) {
        requireNonNull(uuid, "uuid");
        requireNonNull(location, "location");

        return mutations.submit(() -> {
            OfflineLocationDocument next;
            synchronized (this) {
                next = new OfflineLocationDocument();
                next.locations = new LinkedHashMap<>(document.locations);
            }

            next.locations.put(uuid.toString(), location);
            return storage
                    .save(path, next)
                    .thenRun(() -> {
                        synchronized (this) {
                            document = next;
                        }
                    });
        });
    }

    @Override
    public synchronized Optional<CellLocation> location(UUID uuid) {
        return Optional.ofNullable(document.locations.get(
                requireNonNull(uuid, "uuid").toString()
        ));
    }

    @Override
    public void stopAccepting() {
        mutations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return mutations.drain();
    }

}
