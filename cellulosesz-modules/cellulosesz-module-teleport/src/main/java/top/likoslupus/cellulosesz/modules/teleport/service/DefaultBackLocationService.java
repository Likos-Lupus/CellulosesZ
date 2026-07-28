package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.BackLocationService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.teleport.data.BackLocationDocument;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class DefaultBackLocationService implements BackLocationService, AsyncInitializable {

    private final PlatformService platform;
    private final StorageService storage;
    private final Path path;
    private BackLocationDocument document;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public DefaultBackLocationService(
            PlatformService platform,
            StorageService storage,
            Path path
    ) {
        this.platform = platform;
        this.storage = storage;
        this.path = path;
        this.document = new BackLocationDocument();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(path, BackLocationDocument.class, BackLocationDocument::new)
                .thenApply(loaded -> {
                    validate(loaded);
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = loaded;
                    }
                });
    }

    private void validate(BackLocationDocument candidate) {
        //noinspection ResultOfMethodCallIgnored
        candidate.locations.forEach((uuid, _) -> UUID.fromString(uuid));
    }

    @Override
    public CompletableFuture<Void> remember(CellPlayer player) {
        return remember(player.uuid(), platform.location(player));
    }

    @Override
    public CompletableFuture<Void> remember(UUID uuid, CellLocation location) {
        return mutate(next -> next.locations.put(uuid.toString(), location));
    }

    @Override
    public CompletableFuture<Void> forget(UUID uuid) {
        return mutate(next -> next.locations.remove(uuid.toString()));
    }

    @Override
    public synchronized Optional<CellLocation> location(UUID uuid) {
        return Optional.ofNullable(document.locations.get(uuid.toString()));
    }

    private synchronized CompletableFuture<Void> mutate(Consumer<BackLocationDocument> mutation) {
        var result = new CompletableFuture<Void>();
        mutationTail = mutationTail.handle((_, _) -> null)
                .thenCompose(_ -> {
                    BackLocationDocument next;
                    synchronized (this) {
                        next = copy(document);
                    }
                    mutation.accept(next);
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

    private BackLocationDocument copy(BackLocationDocument source) {
        var copy = new BackLocationDocument();
        copy.locations = new LinkedHashMap<>(source.locations);
        return copy;
    }

}
