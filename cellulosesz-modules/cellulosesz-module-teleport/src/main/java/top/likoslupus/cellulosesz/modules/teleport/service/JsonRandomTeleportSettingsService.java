package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettings;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;
import top.likoslupus.cellulosesz.modules.teleport.data.RandomTeleportSettingsDocument;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public final class JsonRandomTeleportSettingsService implements RandomTeleportSettingsService {

    private final StorageService storage;
    private final Path path;
    private final RandomTeleportSettings defaults;
    private RandomTeleportSettingsDocument document;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonRandomTeleportSettingsService(
            StorageService storage,
            Path path,
            RandomTeleportSettings defaults
    ) {
        this.storage = storage;
        this.path = path;
        this.defaults = validate(defaults);
        this.document = storage.load(
                path,
                RandomTeleportSettingsDocument.class,
                RandomTeleportSettingsDocument::new
        ).join();
        validate(document);
    }

    private RandomTeleportSettings validate(RandomTeleportSettings settings) {
        if (!Double.isFinite(settings.centerX()) || !Double.isFinite(settings.centerZ())) {
            throw new IllegalArgumentException("Random teleport center must be finite");
        }
        if (settings.minRadius() < 0 || settings.maxRadius() < 1
                || settings.minRadius() >= settings.maxRadius()) {
            throw new IllegalArgumentException("Random teleport radius range is invalid");
        }
        return settings;
    }

    private void validate(RandomTeleportSettingsDocument candidate) {
        if (candidate.worlds == null) {
            throw new IllegalArgumentException("Random teleport settings are missing worlds");
        }
        candidate.worlds.replaceAll((world, settings) -> validate(settings));
    }

    @Override
    public synchronized RandomTeleportSettings settings(String world) {
        return document.worlds.getOrDefault(normalize(world), defaults);
    }

    @Override
    public CompletableFuture<Void> setCenter(String world, double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Random teleport center must be finite"));
        }
        return update(world, old -> new RandomTeleportSettings(x, z, old.minRadius(), old.maxRadius()));
    }

    @Override
    public CompletableFuture<Void> setMinimumRadius(String world, int radius) {
        return update(world, old -> new RandomTeleportSettings(old.centerX(), old.centerZ(), radius, old.maxRadius()));
    }

    @Override
    public CompletableFuture<Void> setMaximumRadius(String world, int radius) {
        return update(world, old -> new RandomTeleportSettings(old.centerX(), old.centerZ(), old.minRadius(), radius));
    }

    private synchronized CompletableFuture<Void> update(
            String world,
            UnaryOperator<RandomTeleportSettings> mutation
    ) {
        var key = normalize(world);
        var result = new CompletableFuture<Void>();
        mutationTail = mutationTail.handle((unused, failure) -> null)
                .thenCompose(unused -> {
                    RandomTeleportSettingsDocument next;
                    synchronized (this) {
                        next = copy(document);
                    }
                    var previous = next.worlds.getOrDefault(key, defaults);
                    next.worlds.put(key, validate(mutation.apply(previous)));
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

    private RandomTeleportSettingsDocument copy(RandomTeleportSettingsDocument source) {
        var copy = new RandomTeleportSettingsDocument();
        copy.worlds = new LinkedHashMap<>(source.worlds);
        return copy;
    }

    private String normalize(String world) {
        var normalized = world.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("world must not be blank");
        return normalized;
    }

}
