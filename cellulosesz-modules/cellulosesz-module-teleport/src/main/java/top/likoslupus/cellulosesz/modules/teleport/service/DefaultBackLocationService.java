package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
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

import static java.util.Objects.requireNonNull;

public final class DefaultBackLocationService implements BackLocationService, AsyncInitializable {

    private final PlayerLocationPlatformService locations;
    private final StorageService storage;
    private final Path path;
    private BackLocationDocument document = new BackLocationDocument();
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public DefaultBackLocationService(
            PlayerLocationPlatformService locations,
            StorageService storage,
            Path path
    ) {
        this.locations = requireNonNull(locations, "locations");
        this.storage = requireNonNull(storage, "storage");
        this.path = requireNonNull(path, "path");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(
                        path,
                        BackLocationDocument.class,
                        BackLocationDocument::new
                )
                .thenApply(loaded -> {
                    validate(loaded);
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = copy(loaded);
                    }
                });
    }

    private void validate(BackLocationDocument candidate) {
        candidate.locations.forEach((uuid, location) -> {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(uuid);
            validateLocation(location);
        });
    }

    private static BackLocationDocument copy(BackLocationDocument source) {
        var result = new BackLocationDocument();
        var values = new LinkedHashMap<String, CellLocation>();

        source.locations
                .forEach((key, value) -> values.put(key, copy(value)));
        result.locations = values;

        return result;
    }

    private static void validateLocation(CellLocation value) {
        requireNonNull(value, "location");
        if (value.world.isBlank()
                || !Double.isFinite(value.x)
                || !Double.isFinite(value.y)
                || !Double.isFinite(value.z)
        ) {
            throw new IllegalArgumentException("invalid back location");
        }
    }

    private static CellLocation copy(CellLocation value) {
        return new CellLocation(
                value.world,
                value.x, value.y, value.z,
                value.yaw, value.pitch
        );
    }

    @Override
    public CompletableFuture<Void> remember(CellPlayer player) {
        return remember(
                player.uuid(),
                locations.currentLocation(player)
        );
    }

    @Override
    public CompletableFuture<Void> remember(UUID uuid, CellLocation location) {
        requireNonNull(uuid, "uuid");
        validateLocation(location);

        var snapshot = copy(location);
        return mutate(next ->
                next.locations.put(uuid.toString(), snapshot)
        );
    }

    @Override
    public CompletableFuture<Void> forget(UUID uuid) {
        requireNonNull(uuid, "uuid");
        return mutate(next ->
                next.locations.remove(uuid.toString())
        );
    }

    @Override
    public synchronized Optional<CellLocation> location(UUID uuid) {
        return Optional.ofNullable(document.locations.get(uuid.toString()))
                .map(DefaultBackLocationService::copy);
    }

    private synchronized CompletableFuture<Void> mutate(
            Consumer<BackLocationDocument> mutation
    ) {
        var result = new CompletableFuture<Void>();
        mutationTail = mutationTail
                .handle((_, _) -> null)
                .thenCompose(_ -> {
                    final BackLocationDocument next;
                    synchronized (this) {
                        next = copy(document);
                    }

                    mutation.accept(next);
                    return storage
                            .save(path, next)
                            .thenRun(() -> {
                                synchronized (this) {
                                    document = next;
                                }
                            });
                });

        mutationTail
                .whenComplete((_, failure) -> {
                    if (failure == null) {
                        result.complete(null);
                    } else {
                        result.completeExceptionally(failure);
                    }
                });

        return result;
    }

}
