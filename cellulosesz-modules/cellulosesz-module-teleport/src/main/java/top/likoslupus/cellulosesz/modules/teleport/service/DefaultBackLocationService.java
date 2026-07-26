package top.likoslupus.cellulosesz.modules.teleport.service;

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

public final class DefaultBackLocationService implements BackLocationService {

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
        this.document = storage.load(path, BackLocationDocument.class, BackLocationDocument::new).join();
        validate(document);
    }

    private void validate(BackLocationDocument candidate) {
        if (candidate.locations == null) {
            throw new IllegalArgumentException("Back location document is missing locations");
        }
        candidate.locations.forEach((uuid, location) -> {
            UUID.fromString(uuid);
            if (location == null) throw new IllegalArgumentException("Back location must not be null");
        });
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
        mutationTail = mutationTail.handle((unused, failure) -> null)
                .thenCompose(unused -> {
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
        mutationTail.whenComplete((unused, failure) -> {
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
