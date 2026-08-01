package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.BackLocationService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.teleport.data.BackLocationDocument;

import java.nio.file.Path;
import java.util.LinkedHashMap;
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
            validateLocation(location);
        });
    }

    private static BackLocationDocument copy(BackLocationDocument source) {
        var result = new BackLocationDocument();
        var values = new LinkedHashMap<String, CellLocation>();

        source.locations.forEach((key, value) ->
                values.put(key, copy(value))
        );
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
        return mutations.submit(() -> {
            var uuid = player.uuid();
            var location = locations.currentLocation(player);
            validateLocation(location);

            return mutateAccepted(next -> next.locations.put(
                    uuid.toString(),
                    copy(location)
            ));
        });
    }

    @Override
    public CompletableFuture<Void> remember(UUID uuid, CellLocation location) {
        return mutations.submit(() -> {
            requireNonNull(uuid, "uuid");
            validateLocation(location);

            var snapshot = copy(location);
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
                .map(DefaultBackLocationService::copy);
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
