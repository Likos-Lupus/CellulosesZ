package top.likoslupus.cellulosesz.modules.user.service;

import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonUserService implements UserService {

    private final StorageService storage;
    private final NameCacheService nameCache;
    private final Path usersDirectory;
    private final CellulosesZLogger logger;
    private final ConcurrentHashMap<UUID, CellUser> users = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    public JsonUserService(
            StorageService storage,
            NameCacheService nameCache,
            Path usersDirectory,
            CellulosesZLogger logger
    ) {
        this.storage = storage;
        this.nameCache = nameCache;
        this.usersDirectory = usersDirectory;
        this.logger = logger;
    }

    public void markQuit(Object player) {
        var uuid = PlayerIdentity.uuid(player);
        if (uuid.isEmpty()) return;

        var user = users.get(uuid.get());
        if (user == null) return;

        user.timestamps.lastQuit = System.currentTimeMillis();
        markDirty(uuid.get());
    }

    @Override
    public CompletableFuture<CellUser> load(UUID uuid) {
        var cached = users.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return storage.load(userPath(uuid), CellUser.class, () -> create(uuid))
                .thenApply(user -> {
                    normalize(user, uuid);
                    users.put(uuid, user);
                    if (user.lastKnownName != null) {
                        nameCache.remember(uuid, user.lastKnownName);
                    }
                    return user;
                });
    }

    @Override
    public CompletableFuture<CellUser> loadFromPlayer(Object player) {
        var resolvedUuid = PlayerIdentity.uuid(player);
        if (resolvedUuid.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unable to resolve player UUID from " + player));
        }

        var uuid = resolvedUuid.get();
        var name = PlayerIdentity.name(player).orElse(null);
        return load(uuid).thenApply(user -> {
            var now = System.currentTimeMillis();
            if (user.timestamps.firstJoin <= 0L) {
                user.timestamps.firstJoin = now;
            }

            user.timestamps.lastJoin = now;
            if (name != null) {
                user.lastKnownName = name;
                nameCache.remember(uuid, name);
            }

            markDirty(uuid);
            return user;
        });
    }

    @Override
    public Optional<CellUser> cached(UUID uuid) {
        return Optional.ofNullable(users.get(uuid));
    }

    @Override
    public Collection<CellUser> cachedUsers() {
        return List.copyOf(users.values());
    }

    @Override
    public Optional<UUID> findUuidByName(String name) {
        return nameCache.findUuid(name);
    }

    @Override
    public void markDirty(UUID uuid) {
        dirty.add(uuid);
    }


    @Override
    public CompletableFuture<Void> save(UUID uuid) {
        var user = users.get(uuid);
        if (user == null) {
            dirty.remove(uuid);
            return CompletableFuture.completedFuture(null);
        }

        return storage.save(userPath(uuid), user)
                .thenRun(() -> dirty.remove(uuid));
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        var userFutures = dirty.stream()
                .map(this::save)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(userFutures)
                .thenCompose(_ -> nameCache.save())
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to save user data", exception);
                    }
                });
    }

    private CellUser create(UUID uuid) {
        var user = new CellUser(uuid);
        var now = System.currentTimeMillis();
        user.timestamps.firstJoin = now;
        return user;
    }

    private void normalize(CellUser user, UUID fallbackUuid) {
        if (user.uuid.getLeastSignificantBits() == 0L && user.uuid.getMostSignificantBits() == 0L) {
            user.uuid = fallbackUuid;
        }
    }

    private Path userPath(UUID uuid) {
        return usersDirectory.resolve(uuid + ".json");
    }

}
