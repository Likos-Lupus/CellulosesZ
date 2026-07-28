package top.likoslupus.cellulosesz.modules.user.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;

import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.modules.user.data.NameCacheDocument;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultNameCacheService implements NameCacheService, AsyncInitializable {

    private final StorageService storage;
    private final Path path;
    private final ConcurrentHashMap<String, UUID> nameToUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> uuidToName = new ConcurrentHashMap<>();

    public DefaultNameCacheService(
            StorageService storage,
            Path path
    ) {
        this.storage = storage;
        this.path = path;

    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(path, NameCacheDocument.class, NameCacheDocument::new)
                .thenAccept(document ->
                        document.names
                                .forEach((uuid, name) ->
                                        remember(UUID.fromString(uuid), name)
                                )
                );
    }

    @Override
    public void remember(UUID uuid, String name) {
        if (name.isBlank()) return;
        nameToUuid.put(normalize(name), uuid);
        uuidToName.put(uuid, name);
    }

    @Override
    public Optional<UUID> findUuid(String name) {
        return Optional.ofNullable(nameToUuid.get(normalize(name)));
    }

    @Override
    public Optional<String> findName(UUID uuid) {
        return Optional.ofNullable(uuidToName.get(uuid));
    }

    @Override
    public Map<UUID, String> entries() {
        return Map.copyOf(uuidToName);
    }

    @Override
    public CompletableFuture<Void> save() {
        var document = new NameCacheDocument();
        uuidToName.forEach((uuid, name) -> document.names.put(uuid.toString(), name));
        return storage.save(path, document);
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

}
