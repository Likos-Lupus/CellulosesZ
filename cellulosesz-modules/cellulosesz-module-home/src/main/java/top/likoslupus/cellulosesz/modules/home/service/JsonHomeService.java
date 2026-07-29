package top.likoslupus.cellulosesz.modules.home.service;

import top.likoslupus.cellulosesz.api.home.HomeRenameStatus;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public final class JsonHomeService implements HomeService {

    private final StorageService storage;
    private final Path homesDirectory;
    private final ConcurrentHashMap<UUID, HomeDocument> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> writeTails = new ConcurrentHashMap<>();

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
    public Map<String, CellLocation> cachedHomes(UUID uuid) {
        var document = cache.get(uuid);
        return document == null
                ? Map.of()
                : Map.copyOf(document.homes);
    }

    @Override
    public CompletableFuture<Optional<CellLocation>> home(UUID uuid, String name) {
        return document(uuid).thenApply(document ->
                Optional.ofNullable(document.homes.get(normalize(name)))
        );
    }

    @Override
    public CompletableFuture<Boolean> setHome(
            UUID uuid,
            String name,
            CellLocation location
    ) {
        return mutate(uuid, candidate -> {
            candidate.homes.put(normalize(name), location);
            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteHome(UUID uuid, String name) {
        return mutate(
                uuid,
                candidate -> candidate.homes.remove(normalize(name)) != null
        );
    }

    @Override
    public CompletableFuture<HomeRenameStatus> renameHomeDetailed(
            UUID uuid,
            String oldName,
            String newName
    ) {
        return mutate(
                uuid,
                candidate -> {
                    var oldKey = normalize(oldName);
                    var newKey = normalize(newName);

                    if (!candidate.homes.containsKey(oldKey)) return HomeRenameStatus.SOURCE_MISSING;
                    if (candidate.homes.containsKey(newKey)) return HomeRenameStatus.TARGET_EXISTS;

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
        var result = new CompletableFuture<T>();
        var next = writeTails.compute(
                uuid,
                (_, previous) -> {
                    var tail = previous == null
                            ? CompletableFuture.completedFuture(null)
                            : previous;

                    return tail.handle((_, _) -> null)
                            .thenCompose(_ -> document(uuid))
                            .thenCompose(current -> {
                                var candidate = copy(current);
                                final T value;

                                try {
                                    value = mutation.apply(candidate);
                                } catch (RuntimeException failure) {
                                    return CompletableFuture.failedFuture(failure);
                                }

                                if (!shouldPersist.test(value)) {
                                    result.complete(value);
                                    return CompletableFuture.completedFuture(null);
                                }

                                return storage.save(
                                        path(uuid),
                                        candidate
                                ).thenRun(() -> {
                                    cache.put(uuid, candidate);
                                    result.complete(value);
                                });
                            });
                }
        );

        next.whenComplete((_, failure) -> {
            writeTails.remove(uuid, next);
            if (failure != null) result.completeExceptionally(unwrap(failure));
        });

        return result;
    }

    private HomeDocument copy(HomeDocument source) {
        var copy = new HomeDocument(source.uuid);
        copy.homes.putAll(source.homes);
        return copy;
    }

    private Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException completion && completion.getCause() != null
                ? completion.getCause()
                : failure;
    }

    private String normalize(String name) {
        var normalized = name.isBlank()
                ? "home"
                : name.trim();
        return normalized.toLowerCase(Locale.ROOT);
    }

    private CompletableFuture<HomeDocument> document(UUID uuid) {
        var cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return storage.createIfMissing(
                        path(uuid),
                        HomeDocument.class,
                        () -> new HomeDocument(uuid)
                )
                .thenApply(document -> {
                    if (!document.uuid.equals(uuid)) {
                        throw new IllegalArgumentException("Home document UUID does not match its file name");
                    }
                    cache.put(uuid, document);
                    return document;
                });
    }

    private Path path(UUID uuid) {
        return homesDirectory.resolve(uuid + ".json");
    }

}
