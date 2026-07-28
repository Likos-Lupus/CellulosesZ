package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;

import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.OfflineLocationService;
import top.likoslupus.cellulosesz.modules.teleport.data.OfflineLocationDocument;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JsonOfflineLocationService implements OfflineLocationService, AsyncInitializable {

    private final StorageService storage;
    private final Path path;
    private OfflineLocationDocument document;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonOfflineLocationService(StorageService storage, Path path) {
        this.storage = storage;
        this.path = path;
        document = new OfflineLocationDocument();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(path, OfflineLocationDocument.class, OfflineLocationDocument::new)
                .thenApply(loaded -> loaded)
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = loaded;
                    }
                });
    }

    @Override
    public synchronized CompletableFuture<Void> remember(UUID uuid, CellLocation location) {
        var result = new CompletableFuture<Void>();
        mutationTail = mutationTail.handle((_, _) -> null)
                .thenCompose(_ -> {
                    OfflineLocationDocument next;
                    synchronized (this) {
                        next = new OfflineLocationDocument();
                        next.locations = new LinkedHashMap<>(document.locations);
                    }
                    next.locations.put(uuid.toString(), location);
                    return storage.save(path, next).thenRun(() -> {
                        synchronized (this) {
                            document = next;
                        }
                    });
                });
        mutationTail.whenComplete((_, failure) -> {
            if (failure == null) result.complete(null);
            else result.completeExceptionally(failure);
        });
        return result;
    }

    @Override
    public synchronized Optional<CellLocation> location(UUID uuid) {
        return Optional.ofNullable(document.locations.get(uuid.toString()));
    }

}
