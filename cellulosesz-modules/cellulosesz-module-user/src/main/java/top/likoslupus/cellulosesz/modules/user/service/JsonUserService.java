package top.likoslupus.cellulosesz.modules.user.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.*;
import top.likoslupus.cellulosesz.core.concurrent.KeyedSerialAsyncQueue;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.user.persistence.UserDocument;
import top.likoslupus.cellulosesz.modules.user.persistence.UserMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/** Persist-before-publish immutable user repository. */
public final class JsonUserService implements UserService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_PER_USER = 1_024;
    private static final int MAXIMUM_PENDING_BATCHES = 128;

    private final StorageService storage;
    private final NameCacheService nameCache;
    private final Path usersDirectory;
    private final CellulosesZLogger logger;
    private final Clock clock;
    private final ConcurrentHashMap<UUID, CellUser> users = new ConcurrentHashMap<>();
    private final Set<UUID> knownUuids = ConcurrentHashMap.newKeySet();
    private final KeyedSerialAsyncQueue<UUID> userOperations = new KeyedSerialAsyncQueue<>(
            Runnable::run,
            MAXIMUM_PENDING_PER_USER
    );
    private final SerialAsyncQueue batchOperations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_BATCHES
    );
    private final Object lifecycleLock = new Object();
    private boolean accepting = true;
    private @Nullable CompletableFuture<Void> drainFuture;

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
                .loadDirectory(usersDirectory, UserDocument.class)
                .thenAccept(documents -> documents.stream()
                        .map(UserMapper::toDomain)
                        .forEach(user -> {
                            validate(user, user.uuid());
                            knownUuids.add(user.uuid());

                            if (user.lastKnownName() != null) {
                                nameCache.remember(user.uuid(), user.lastKnownName());
                            }
                        }));
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
                                    + player.uuid()
                            );
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
    public CompletableFuture<CellUser> load(UUID uuid) {
        var key = requireNonNull(uuid, "uuid");
        return enqueueUserOperation(key, () -> loadAccepted(key));
    }

    @Override
    public CompletableFuture<CellUser> loadFromPlayer(CellPlayer player) {
        return loadFromPlayer(player, true);
    }

    public CompletableFuture<CellUser> loadFromPlayer(
            CellPlayer player,
            boolean updateNameCache
    ) {
        requireNonNull(player, "player");
        return update(
                player.uuid(),
                user -> {
                    var now = clock.millis();
                    var timestamps = user.timestamps();
                    var firstJoin = timestamps.firstJoin() <= 0
                            ? now
                            : timestamps.firstJoin();
                    var updated = user.withTimestamps(new UserTimestamps(
                            firstJoin,
                            now,
                            timestamps.lastQuit(),
                            timestamps.playTimeMillis(),
                            now,
                            now
                    ));
                    if (updateNameCache) {
                        updated = updated.withLastKnownName(player.name());
                    }
                    return UserUpdate.of(updated, updated);
                }
        );
    }

    @Override
    public Optional<CellUser> cached(UUID uuid) {
        return Optional.ofNullable(users.get(requireNonNull(uuid, "uuid")));
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
        var key = requireNonNull(uuid, "uuid");
        requireNonNull(mutation, "mutation");
        return enqueueUserOperation(
                key,
                () -> loadAccepted(key).thenCompose(current -> {
                    var update = requireNonNull(mutation.apply(current), "update");
                    validate(update.user(), key);

                    return storage
                            .save(userPath(key), UserMapper.fromDomain(update.user()))
                            .thenApply(_ -> {
                                publish(key, update.user());
                                // FIXME: result() is nullable
                                return update.result();
                            });
                })
        );
    }

    @Override
    public CompletableFuture<Void> save(UUID uuid) {
        var key = requireNonNull(uuid, "uuid");
        return enqueueUserOperation(key, () -> saveAccepted(key));
    }

    private CompletableFuture<Void> saveAccepted(UUID uuid) {
        var user = users.get(uuid);
        return user == null
                ? CompletableFuture.completedFuture(null)
                : storage.save(userPath(uuid), UserMapper.fromDomain(user));
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        final CompletableFuture<Void> accepted;
        synchronized (lifecycleLock) {
            if (!accepting) {
                return closedFuture();
            }

            var snapshot = Set.copyOf(users.keySet());
            var saves = snapshot.stream()
                    .map(uuid -> userOperations.submit(uuid, () -> saveAccepted(uuid)))
                    .toArray(CompletableFuture[]::new);
            accepted = batchOperations.submit(() -> CompletableFuture
                    .allOf(saves)
                    .thenCompose(_ -> nameCache.save()));
        }

        return accepted.whenComplete((_, failure) -> {
            if (failure != null) {
                logger.error("Failed to save user data", unwrap(failure));
            }
        });
    }

    private static <T> CompletableFuture<T> closedFuture() {
        return CompletableFuture.failedFuture(new IllegalStateException("User service is closing"));
    }

    private static Throwable unwrap(Throwable failure) {
        var current = failure;
        while (current instanceof CompletionException
                && current.getCause() != null
        ) {
            current = current.getCause();
        }
        return current;
    }

    private <T> CompletableFuture<T> enqueueUserOperation(
            UUID uuid,
            Supplier<? extends CompletableFuture<T>> operation
    ) {
        synchronized (lifecycleLock) {
            if (!accepting) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "User service is closing"
                ));
            }

            return userOperations.submit(uuid, operation);
        }
    }

    private CompletableFuture<CellUser> loadAccepted(UUID uuid) {
        var cached = users.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return storage.createIfMissing(
                        userPath(uuid),
                        UserDocument.class,
                        () -> UserMapper.fromDomain(create(uuid))
                )
                .thenApply(UserMapper::toDomain)
                .thenApply(user -> {
                    validate(user, uuid);
                    var normalized = user.timestamps().activeSessionStartedAt() == null
                            ? user
                            : user.withTimestamps(
                                    user.timestamps().withActiveSessionStartedAt(null)
                            );
                    publish(uuid, normalized);
                    return normalized;
                });
    }

    private Path userPath(UUID uuid) {
        return usersDirectory.resolve(uuid + ".json");
    }

    private CellUser create(UUID uuid) {
        var now = clock.millis();
        return CellUser.create(uuid)
                .withTimestamps(UserTimestamps.defaults().withFirstJoin(now));
    }

    private void publish(UUID uuid, CellUser user) {
        users.put(uuid, user);
        knownUuids.add(uuid);

        if (user.lastKnownName() != null) {
            nameCache.remember(uuid, user.lastKnownName());
        }
    }

    @Override
    public void stopAccepting() {
        synchronized (lifecycleLock) {
            if (!accepting) {
                return;
            }

            accepting = false;
            userOperations.stopAccepting();
            batchOperations.stopAccepting();
        }
    }

    @Override
    public CompletableFuture<Void> drain() {
        synchronized (lifecycleLock) {
            if (!accepting && drainFuture != null) {
                return drainFuture;
            }

            var accepted = CompletableFuture.allOf(
                    userOperations.drain(),
                    batchOperations.drain()
            );

            var barrier = accepted
                    .handle((_, failure) -> failure == null
                            ? null
                            : unwrap(failure))
                    .thenCompose(acceptedFailure -> nameCache.save()
                            .handle((_, nameFailure) -> {
                                if (acceptedFailure == null && nameFailure == null) {
                                    return (Void) null;
                                }

                                var aggregate = new IllegalStateException(
                                        "User service drain failed"
                                );
                                if (acceptedFailure != null) {
                                    aggregate.addSuppressed(acceptedFailure);
                                }
                                if (nameFailure != null) {
                                    aggregate.addSuppressed(unwrap(nameFailure));
                                }

                                throw new CompletionException(aggregate);
                            })
                    );

            if (!accepting) {
                drainFuture = barrier;
            }

            return barrier;
        }
    }

}
