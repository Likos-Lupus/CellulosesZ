package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncCloseable;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncInitializable;
import top.likoslupus.cellulosesz.core.storage.StorageService;
import top.likoslupus.cellulosesz.modules.teleport.persistence.BackLocationDocument;
import top.likoslupus.cellulosesz.modules.teleport.persistence.LocationMapper;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class DefaultBackLocationService
        implements BackLocationService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_MUTATIONS = 4_096;

    private final PlayerLocationPlatformService locations;
    private final StorageService storage;
    private final Path path;
    private final SerialAsyncQueue mutations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_MUTATIONS
    );
    private BackLocationDocument document = new BackLocationDocument();

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
        return storage
                .createIfMissing(
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
            LocationMapper.toDomain(location, uuid);
        });
    }

    private static BackLocationDocument copy(BackLocationDocument source) {
        var result = new BackLocationDocument();
        source.locations.forEach((key, value) ->
                result.locations.put(key, LocationMapper.copy(value))
        );
        return result;
    }

    @Override
    public CompletableFuture<Void> remember(CellPlayer player) {
        return mutations.submit(() -> {
            var uuid = player.uuid();
            var location = requireNonNull(
                    locations.currentLocation(player),
                    "location"
            );
            return mutateAccepted(next -> next.locations.put(
                    uuid.toString(),
                    LocationMapper.fromDomain(location)
            ));
        });
    }

    @Override
    public CompletableFuture<Void> remember(UUID uuid, CellLocation location) {
        return mutations.submit(() -> {
            requireNonNull(uuid, "uuid");
            var snapshot = LocationMapper.fromDomain(
                    requireNonNull(location, "location")
            );
            return mutateAccepted(next ->
                    next.locations.put(uuid.toString(), snapshot)
            );
        });
    }

    @Override
    public CompletableFuture<Void> forget(UUID uuid) {
        return mutations.submit(() -> {
            requireNonNull(uuid, "uuid");
            return mutateAccepted(next ->
                    next.locations.remove(uuid.toString())
            );
        });
    }

    @Override
    public synchronized Optional<CellLocation> location(UUID uuid) {
        return Optional.ofNullable(document.locations.get(uuid.toString()))
                .map(value -> LocationMapper.toDomain(value, uuid.toString()));
    }

    private CompletableFuture<Void> mutateAccepted(
            Consumer<BackLocationDocument> mutation
    ) {
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
