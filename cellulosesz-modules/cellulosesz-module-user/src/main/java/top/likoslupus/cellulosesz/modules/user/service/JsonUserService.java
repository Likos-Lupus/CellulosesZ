package top.likoslupus.cellulosesz.modules.user.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.*;

import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/** Persist-before-publish immutable user repository. */
public final class JsonUserService implements UserService, AsyncInitializable, AsyncCloseable {

    private final StorageService storage;
    private final NameCacheService nameCache;
    private final Path usersDirectory;
    private final CellulosesZLogger logger;
    private final Clock clock;
    private final ConcurrentHashMap<UUID, CellUser> users = new ConcurrentHashMap<>();
    private final Set<UUID> knownUuids = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, CompletableFuture<CellUser>> loadFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> updateTails = new ConcurrentHashMap<>();
    private final AtomicBoolean closing = new AtomicBoolean();

    public JsonUserService(
            StorageService storage,
            NameCacheService nameCache,
            Path usersDirectory,
            CellulosesZLogger logger
    ) {
        this(storage, nameCache, usersDirectory, logger, Clock.systemUTC());
    }

    JsonUserService(
            StorageService storage,
            NameCacheService nameCache,
            Path usersDirectory,
            CellulosesZLogger logger,
            Clock clock
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.nameCache = requireNonNull(nameCache, "nameCache");
        this.usersDirectory = requireNonNull(usersDirectory, "usersDirectory");
        this.logger = requireNonNull(logger, "logger");
        this.clock = requireNonNull(clock, "clock");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage
                .loadDirectory(usersDirectory, CellUser.class)
                .thenAccept(documents -> documents
                        .forEach(user -> {
                            validate(user, user.uuid());
                            knownUuids.add(user.uuid());
                            if (user.lastKnownName() != null) {
                                nameCache.remember(user.uuid(), user.lastKnownName());
                            }
                        })
                );
    }

    private static void validate(CellUser user, UUID expectedUuid) {
        requireNonNull(user, "user");
        if (!expectedUuid.equals(user.uuid())) {
            throw new IllegalArgumentException("User document UUID does not match its file name");
        }
    }

    public CompletableFuture<Void> markQuit(CellPlayer player) {
        requireNonNull(player, "player");
        return updateVoid(
                player.uuid(),
                user -> {
                    var now = clock.millis();
                    var timestamps = user.timestamps();
                    var playTime = timestamps.playTimeMillis();

                    if (timestamps.activeSessionStartedAt() != null) {
                        var elapsed = Math.max(0L, now - timestamps.activeSessionStartedAt());
                        try {
                            playTime = Math.addExact(playTime, elapsed);
                        } catch (ArithmeticException overflow) {
                            playTime = Long.MAX_VALUE;
                            logger.warn("User play time overflowed and was saturated for "
                                    + player.uuid());
                        }
                    }

                    return user.withTimestamps(new UserTimestamps(
                            timestamps.firstJoin(),
                            timestamps.lastJoin(),
                            now,
                            playTime,
                            timestamps.lastActivityAt(),
                            null
                    ));
                }
        );
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        closing.set(true);
        var loads = CompletableFuture.allOf(
                loadFutures
                        .values()
                        .toArray(CompletableFuture[]::new)
        );
        var updates = CompletableFuture.allOf(
                updateTails
                        .values()
                        .toArray(CompletableFuture[]::new)
        );

        return CompletableFuture.allOf(loads, updates)
                .thenCompose(_ -> nameCache.save());
    }

    private Path userPath(UUID uuid) {
        return usersDirectory.resolve(uuid + ".json");
    }

    @Override
    public CompletableFuture<CellUser> load(UUID uuid) {
        requireNonNull(uuid, "uuid");
        var cached = users.get(uuid);

        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        if (closing.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "User service is closing"
            ));
        }

        return loadFutures.computeIfAbsent(
                uuid,
                key -> {
                    var created = storage
                            .createIfMissing(
                                    userPath(key),
                                    CellUser.class,
                                    () -> create(key)
                            )
                            .thenApply(user -> {
                                validate(user, key);
                                var normalized = user.timestamps().activeSessionStartedAt() == null
                                        ? user
                                        : user.withTimestamps(
                                                user
                                                        .timestamps()
                                                        .withActiveSessionStartedAt(null)
                                        );

                                users.put(key, normalized);
                                knownUuids.add(key);

                                if (normalized.lastKnownName() != null) {
                                    nameCache.remember(key, normalized.lastKnownName());
                                }

                                return normalized;
                            });
                    created.whenComplete((_, _) -> loadFutures.remove(key, created));
                    return created;
                }
        );
    }

    @Override
    public CompletableFuture<CellUser> loadFromPlayer(CellPlayer player) {
        requireNonNull(player, "player");
        return update(
                player.uuid(),
                user -> {
                    var now = clock.millis();
                    var timestamps = user.timestamps();
                    var firstJoin = timestamps.firstJoin() <= 0
                            ? now
                            : timestamps.firstJoin();
                    var updated = user
                            .withLastKnownName(player.name())
                            .withTimestamps(new UserTimestamps(
                                    firstJoin,
                                    now,
                                    timestamps.lastQuit(),
                                    timestamps.playTimeMillis(),
                                    now,
                                    now
                            ));

                    return UserUpdate.of(updated, updated);
                }
        ).thenApply(user -> {
            nameCache.remember(player.uuid(), player.name());
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
    public Collection<UUID> knownUuids() {
        var known = new LinkedHashSet<>(knownUuids);
        known.addAll(nameCache.entries().keySet());
        known.addAll(users.keySet());
        return Set.copyOf(known);
    }

    @Override
    public <T> CompletableFuture<T> update(
            UUID uuid,
            Function<CellUser, UserUpdate<T>> mutation
    ) {
        requireNonNull(uuid, "uuid");
        requireNonNull(mutation, "mutation");

        if (closing.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "User service is closing"
            ));
        }

        var result = new CompletableFuture<T>();
        updateTails.compute(
                uuid,
                (_, previous) -> {
                    var tail = previous == null
                            ? CompletableFuture.<Void>completedFuture(null)
                            : previous;
                    var next = tail
                            .handle((_, _) -> null)
                            .thenCompose(_ -> load(uuid))
                            .thenCompose(current -> {
                                final UserUpdate<T> update;
                                try {
                                    update = requireNonNull(mutation.apply(current), "update");
                                    validate(update.user(), uuid);
                                } catch (RuntimeException exception) {
                                    return CompletableFuture.failedFuture(exception);
                                }

                                return storage
                                        .save(userPath(uuid), update.user())
                                        .thenRun(() -> {
                                            users.put(uuid, update.user());
                                            result.complete(update.result());
                                        });
                            });

                    next.whenComplete((_, failure) -> {
                        updateTails.remove(uuid, next);
                        if (failure != null) {
                            result.completeExceptionally(failure);
                        }
                    });

                    return next;
                }
        );

        return result;
    }

    @Override
    public CompletableFuture<Void> save(UUID uuid) {
        var user = users.get(uuid);
        return user == null
                ? CompletableFuture.completedFuture(null)
                : storage.save(userPath(uuid), user);
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        return CompletableFuture
                .allOf(users.keySet().stream()
                        .map(this::save)
                        .toArray(CompletableFuture[]::new)
                )
                .thenCompose(_ -> nameCache.save())
                .whenComplete((_, failure) -> {
                    if (failure != null) {
                        logger.error("Failed to save user data", failure);
                    }
                });
    }

    private CellUser create(UUID uuid) {
        var now = clock.millis();
        return CellUser
                .create(uuid)
                .withTimestamps(UserTimestamps.defaults().withFirstJoin(now));
    }

}
