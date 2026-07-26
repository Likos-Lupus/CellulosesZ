package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.OfflineLocationService;
import top.likoslupus.cellulosesz.modules.teleport.data.OfflineLocationDocument;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JsonOfflineLocationService implements OfflineLocationService {

    private final StorageService storage;
    private final Path path;
    private OfflineLocationDocument document;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonOfflineLocationService(StorageService storage, Path path) {
        this.storage = storage;
        this.path = path;
        document = storage.load(path, OfflineLocationDocument.class, OfflineLocationDocument::new).join();
        if (document.locations == null)
            throw new IllegalArgumentException("Offline location document is missing locations");
    }

    @Override
    public synchronized CompletableFuture<Void> remember(UUID uuid, CellLocation location) {
        var result = new CompletableFuture<Void>();
        mutationTail = mutationTail.handle((unused, failure) -> null)
                .thenCompose(unused -> {
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
        mutationTail.whenComplete((unused, failure) -> {
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
