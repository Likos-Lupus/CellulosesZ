package top.likoslupus.cellulosesz.modules.home.service;

import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.home.data.HomeDocument;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonHomeService implements HomeService {

    private final StorageService storage;
    private final Path homesDirectory;
    private final ConcurrentHashMap<UUID, HomeDocument> cache = new ConcurrentHashMap<>();

    public JsonHomeService(
            StorageService storage,
            Path homesDirectory
    ) {
        this.storage = storage;
        this.homesDirectory = homesDirectory;
    }

    @Override
    public CompletableFuture<Map<String, CellLocation>> homes(UUID uuid) {
        return document(uuid).thenApply(document -> Map.copyOf(document.homes));
    }

    @Override
    public CompletableFuture<Optional<CellLocation>> home(UUID uuid, String name) {
        return document(uuid).thenApply(document -> Optional.ofNullable(document.homes.get(normalize(name))));
    }

    @Override
    public CompletableFuture<Boolean> setHome(
            UUID uuid,
            String name,
            CellLocation location
    ) {
        return document(uuid).thenCompose(document -> {
            document.homes.put(normalize(name), location);
            return save(document).thenApply(_ -> true);
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteHome(UUID uuid, String name) {
        return document(uuid).thenCompose(document -> {
            var removed = document.homes.remove(normalize(name)) != null;
            if (!removed) return CompletableFuture.completedFuture(false);
            return save(document).thenApply(_ -> true);
        });
    }

    @Override
    public CompletableFuture<Boolean> renameHome(
            UUID uuid,
            String oldName,
            String newName
    ) {
        return document(uuid).thenCompose(document -> {
            var oldKey = normalize(oldName);
            var newKey = normalize(newName);
            var location = document.homes.remove(oldKey);

            if (location == null || document.homes.containsKey(newKey)) {
                if (location != null) document.homes.put(oldKey, location);
                return CompletableFuture.completedFuture(false);
            }

            document.homes.put(newKey, location);
            return save(document).thenApply(_ -> true);
        });
    }

    private CompletableFuture<Void> save(HomeDocument document) {
        cache.put(document.uuid, document);
        return storage.save(path(document.uuid), document);
    }

    private String normalize(String name) {
        var normalized = name.isBlank() ? "home" : name.trim();
        return normalized.toLowerCase(Locale.ROOT);
    }

    private CompletableFuture<HomeDocument> document(UUID uuid) {
        var cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return storage.load(path(uuid), HomeDocument.class, () -> new HomeDocument(uuid))
                .thenApply(document -> {
                    if (document.uuid.getLeastSignificantBits() == 0L && document.uuid.getMostSignificantBits() == 0L) {
                        document.uuid = uuid;
                    }
                    cache.put(uuid, document);
                    return document;
                });
    }

    private Path path(UUID uuid) {
        return homesDirectory.resolve(uuid + ".json");
    }

}
