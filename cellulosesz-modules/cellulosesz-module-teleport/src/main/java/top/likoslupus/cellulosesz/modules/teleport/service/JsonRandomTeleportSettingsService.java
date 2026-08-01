package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettings;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.teleport.data.RandomTeleportSettingsDocument;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public final class JsonRandomTeleportSettingsService
        implements RandomTeleportSettingsService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_MUTATIONS = 4_096;

    private final StorageService storage;
    private final Path path;
    private final RandomTeleportSettings defaults;
    private final SerialAsyncQueue mutations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_MUTATIONS
    );
    private RandomTeleportSettingsDocument document;

    public JsonRandomTeleportSettingsService(
            StorageService storage,
            Path path,
            RandomTeleportSettings defaults
    ) {
        this.storage = storage;
        this.path = path;
        this.defaults = validate(defaults);
        this.document = new RandomTeleportSettingsDocument();
    }

    private RandomTeleportSettings validate(RandomTeleportSettings settings) {
        if (!Double.isFinite(settings.centerX())
                || !Double.isFinite(settings.centerZ())
        ) {
            throw new IllegalArgumentException("Random teleport center must be finite");
        }

        if (settings.minRadius() < 0 || settings.maxRadius() < 1
                || settings.minRadius() >= settings.maxRadius()
        ) {
            throw new IllegalArgumentException("Random teleport radius range is invalid");
        }

        return settings;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage
                .createIfMissing(
                        path,
                        RandomTeleportSettingsDocument.class,
                        RandomTeleportSettingsDocument::new
                )
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

    private void validate(RandomTeleportSettingsDocument candidate) {
        candidate.worlds.replaceAll((_, settings) -> validate(settings));
    }

    @Override
    public synchronized RandomTeleportSettings settings(String world) {
        return document.worlds.getOrDefault(normalize(world), defaults);
    }

    @Override
    public CompletableFuture<Void> setCenter(
            String world,
            double x, double z
    ) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Random teleport center must be finite"
            ));
        }

        return update(
                world,
                old -> new RandomTeleportSettings(
                        x, z,
                        old.minRadius(),
                        old.maxRadius()
                )
        );
    }

    @Override
    public CompletableFuture<Void> setMinimumRadius(String world, int radius) {
        return update(
                world,
                old -> new RandomTeleportSettings(
                        old.centerX(), old.centerZ(),
                        radius,
                        old.maxRadius()
                )
        );
    }

    @Override
    public CompletableFuture<Void> setMaximumRadius(String world, int radius) {
        return update(
                world,
                old -> new RandomTeleportSettings(
                        old.centerX(), old.centerZ(),
                        old.minRadius(),
                        radius
                )
        );
    }

    private CompletableFuture<Void> update(
            String world,
            UnaryOperator<RandomTeleportSettings> mutation
    ) {
        var key = normalize(world);
        return mutations.submit(() -> {
            RandomTeleportSettingsDocument next;
            synchronized (this) {
                next = copy(document);
            }

            var previous = next.worlds.getOrDefault(key, defaults);
            next.worlds.put(key, validate(mutation.apply(previous)));
            return storage
                    .save(path, next)
                    .thenRun(() -> {
                        synchronized (this) {
                            document = next;
                        }
                    });
        });
    }

    private RandomTeleportSettingsDocument copy(RandomTeleportSettingsDocument source) {
        var copy = new RandomTeleportSettingsDocument();
        copy.worlds = new LinkedHashMap<>(source.worlds);
        return copy;
    }

    private String normalize(String world) {
        var normalized = world.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("world must not be blank");
        }

        return normalized;
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
