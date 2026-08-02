package top.likoslupus.cellulosesz.modules.home.service;

import top.likoslupus.cellulosesz.api.home.HomeRenameStatus;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.core.concurrent.KeyedSerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.home.persistence.HomeDocument;
import top.likoslupus.cellulosesz.modules.home.persistence.HomeMapper;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class JsonHomeService implements HomeService, AsyncCloseable {

    private static final int MAXIMUM_PENDING_PER_PLAYER = 1_024;

    private final StorageService storage;
    private final Path homesDirectory;
    private final ConcurrentHashMap<UUID, HomeDocument> cache = new ConcurrentHashMap<>();
    private final KeyedSerialAsyncQueue<UUID> operations = new KeyedSerialAsyncQueue<>(
            Runnable::run,
            MAXIMUM_PENDING_PER_PLAYER
    );

    public JsonHomeService(StorageService storage, Path homesDirectory) {
        this.storage = requireNonNull(storage, "storage");
        this.homesDirectory = requireNonNull(homesDirectory, "homesDirectory");
    }

    @Override
    public CompletableFuture<Map<String, CellLocation>> homes(UUID uuid) {
        var key = requireNonNull(uuid, "uuid");
        return operations.submit(
                key,
                () -> documentAccepted(key).thenApply(HomeMapper::homes)
        );
    }

    @Override
    public Map<String, CellLocation> cachedHomes(UUID uuid) {
        var document = cache.get(requireNonNull(uuid, "uuid"));
        return document == null
                ? Map.of()
                : HomeMapper.homes(document);
    }

    @Override
    public CompletableFuture<Optional<CellLocation>> home(UUID uuid, String name) {
        var key = requireNonNull(uuid, "uuid");
        var normalizedName = normalize(name);
        return operations.submit(
                key,
                () -> documentAccepted(key)
                        .thenApply(document ->
                                Optional.ofNullable(document.homes.get(normalizedName))
                                        .map(value ->
                                                HomeMapper.toDomain(value, normalizedName)
                                        )
                        )
        );
    }

    @Override
    public CompletableFuture<Boolean> setHome(
            UUID uuid,
            String name,
            CellLocation location
    ) {
        requireNonNull(location, "location");
        var normalizedName = normalize(name);
        return mutate(
                requireNonNull(uuid, "uuid"),
                candidate -> {
                    candidate.homes.put(normalizedName, HomeMapper.fromDomain(location));
                    return true;
                }
        );
    }

    @Override
    public CompletableFuture<Boolean> deleteHome(UUID uuid, String name) {
        var normalizedName = normalize(name);
        return mutate(
                requireNonNull(uuid, "uuid"),
                candidate -> candidate.homes.remove(normalizedName) != null
        );
    }

    @Override
    public CompletableFuture<HomeRenameStatus> renameHomeDetailed(
            UUID uuid,
            String oldName,
            String newName
    ) {
        var oldKey = normalize(oldName);
        var newKey = normalize(newName);

        return mutate(
                requireNonNull(uuid, "uuid"),
                candidate -> {
                    if (!candidate.homes.containsKey(oldKey)) {
                        return HomeRenameStatus.SOURCE_MISSING;
                    }

                    if (candidate.homes.containsKey(newKey)) {
                        return HomeRenameStatus.TARGET_EXISTS;
                    }

                    var location = candidate.homes.remove(oldKey);
                    candidate.homes.put(newKey, location);

                    return HomeRenameStatus.RENAMED;
                },
                status -> status == HomeRenameStatus.RENAMED
        );
    }

    private <T> CompletableFuture<T> mutate(
            UUID uuid,
            Function<HomeDocument, T> mutation
    ) {
        return mutate(uuid, mutation, _ -> true);
    }

    private <T> CompletableFuture<T> mutate(
            UUID uuid,
            Function<HomeDocument, T> mutation,
            Predicate<T> shouldPersist
    ) {
        return operations.submit(
                uuid,
                () -> documentAccepted(uuid).thenCompose(current -> {
                    var candidate = copy(current);
                    var value = mutation.apply(candidate);
                    if (!shouldPersist.test(value)) {
                        return CompletableFuture.completedFuture(value);
                    }

                    return storage
                            .save(path(uuid), candidate)
                            .thenApply(_ -> {
                                cache.put(uuid, candidate);
                                return value;
                            });
                })
        );
    }

    private HomeDocument copy(HomeDocument source) {
        return HomeMapper.copy(source);
    }

    private String normalize(String name) {
        var normalized = requireNonNull(name, "name").isBlank()
                ? "home"
                : name.trim();
        return normalized.toLowerCase(Locale.ROOT);
    }

    private CompletableFuture<HomeDocument> documentAccepted(UUID uuid) {
        var cached = cache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return storage
                .createIfMissing(
                        path(uuid),
                        HomeDocument.class,
                        () -> HomeMapper.empty(uuid)
                )
                .thenApply(document -> {
                    if (!HomeMapper.uuid(document).equals(uuid)) {
                        throw new IllegalArgumentException(
                                "Home document UUID does not match its file name"
                        );
                    }

                    cache.put(uuid, document);
                    return document;
                });
    }

    private Path path(UUID uuid) {
        return homesDirectory.resolve(uuid + ".json");
    }

    @Override
    public void stopAccepting() {
        operations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return operations.drain();
    }

}
