package top.likoslupus.cellulosesz.modules.user.service;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;

import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class JsonUserService implements
        UserService,
        AsyncInitializable,
        AsyncCloseable {

    private final StorageService storage;
    private final NameCacheService nameCache;
    private final Path usersDirectory;
    private final CellulosesZLogger logger;
    private final ConcurrentHashMap<UUID, CellUser> users = new ConcurrentHashMap<>();
    private final Set<UUID> knownUuids = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, CompletableFuture<CellUser>> loadFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> updateTails = new ConcurrentHashMap<>();
    private final AtomicBoolean closing = new AtomicBoolean();

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


    @Override
    public CompletableFuture<Void> initialize() {
        return storage.loadDirectory(usersDirectory, CellUser.class)
                .thenAccept(documents ->
                        documents.forEach(user -> {
                            validate(user, user.uuid, true);
                            knownUuids.add(user.uuid);
                            if (user.lastKnownName != null) {
                                nameCache.remember(user.uuid, user.lastKnownName);
                            }
                        })
                );
    }

    private void validate(
            CellUser user,
            UUID expectedUuid,
            boolean resetInterruptedSession
    ) {
        requireNonNull(user, "user");
        if (!expectedUuid.equals(user.uuid)) {
            throw new IllegalArgumentException("User document UUID does not match its file name");
        }
        requireNonNull(user.timestamps, "timestamps");
        requireNonNull(user.state, "state");
        requireNonNull(user.preferences, "preferences");
        requireNonNull(user.relations, "relations");
        requireNonNull(user.cooldowns, "cooldowns");
        if (user.timestamps.playTimeMillis < 0L) {
            throw new IllegalArgumentException("playTimeMillis must not be negative");
        }
        // Only a document read can contain an interrupted session. Normal in-memory updates must preserve it.
        if (resetInterruptedSession) user.timestamps.activeSessionStartedAt = null;
    }

    public void markQuit(Object player) {
        var uuid = PlayerIdentity.uuid(player);
        if (uuid.isEmpty()) return;

        var user = users.get(uuid.get());
        if (user == null) return;

        var now = System.currentTimeMillis();
        user.timestamps.lastQuit = now;
        if (user.timestamps.activeSessionStartedAt != null) {
            var elapsed = Math.max(0L, now - user.timestamps.activeSessionStartedAt);
            try {
                user.timestamps.playTimeMillis = Math.addExact(user.timestamps.playTimeMillis, elapsed);
            } catch (ArithmeticException overflow) {
                user.timestamps.playTimeMillis = Long.MAX_VALUE;
                logger.warn("User play time overflowed and was saturated for " + uuid.get());
            }
            user.timestamps.activeSessionStartedAt = null;
        }
        markDirty(uuid.get());
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        closing.set(true);
        var loads = CompletableFuture.allOf(loadFutures.values().toArray(CompletableFuture[]::new));
        var updates = CompletableFuture.allOf(updateTails.values().toArray(CompletableFuture[]::new));
        return CompletableFuture.allOf(loads, updates).thenCompose(_ -> saveAll());
    }

    private Path userPath(UUID uuid) {
        return usersDirectory.resolve(uuid + ".json");
    }

    @Override
    public CompletableFuture<CellUser> load(UUID uuid) {
        requireNonNull(uuid, "uuid");
        var cached = users.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        if (closing.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("User service is closing"));
        }

        return loadFutures.computeIfAbsent(
                uuid,
                key -> {
                    var created = storage.createIfMissing(
                                    userPath(key),
                                    CellUser.class,
                                    () -> create(key)
                            )
                            .thenApply(user -> {
                                validate(user, key, true);
                                users.put(key, user);
                                knownUuids.add(key);
                                if (user.lastKnownName != null) {
                                    nameCache.remember(key, user.lastKnownName);
                                }
                                return user;
                            });
                    created.whenComplete((_, _) -> loadFutures.remove(key, created));
                    return created;
                });
    }

    @Override
    public CompletableFuture<CellUser> loadFromPlayer(Object player) {
        var resolvedUuid = PlayerIdentity.uuid(player);
        if (resolvedUuid.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unable to resolve player UUID from " + player));
        }

        var uuid = resolvedUuid.get();
        var name = PlayerIdentity.name(player);
        return load(uuid).thenApply(user -> {
            var now = System.currentTimeMillis();
            if (user.timestamps.firstJoin <= 0L) {
                user.timestamps.firstJoin = now;
            }

            user.timestamps.lastJoin = now;
            user.timestamps.lastActivityAt = now;
            user.timestamps.activeSessionStartedAt = now;
            name.ifPresent(value -> {
                user.lastKnownName = value;
                nameCache.remember(uuid, value);
            });

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
    public Collection<UUID> knownUuids() {
        var known = new LinkedHashSet<>(knownUuids);
        known.addAll(nameCache.entries().keySet());
        known.addAll(users.keySet());
        return Set.copyOf(known);
    }

    @Override
    public <T> CompletableFuture<T> update(UUID uuid, Function<CellUser, T> mutation) {
        requireNonNull(uuid, "uuid");
        requireNonNull(mutation, "mutation");
        if (closing.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("User service is closing"));
        }
        var result = new CompletableFuture<T>();
        updateTails.compute(uuid, (_, previous) -> {
            var tail = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous;
            var next = tail.handle((_, _) -> null)
                    .thenCompose(_ -> load(uuid))
                    .thenCompose(current -> {
                        CellUser replacement;
                        synchronized (current) {
                            replacement = copy(current);
                        }
                        final T value;
                        try {
                            value = mutation.apply(replacement);
                            validate(replacement, uuid, false);
                        } catch (RuntimeException exception) {
                            return CompletableFuture.failedFuture(exception);
                        }
                        return storage.save(userPath(uuid), replacement)
                                .thenRun(() -> {
                                    users.put(uuid, replacement);
                                    dirty.remove(uuid);
                                    result.complete(value);
                                });
                    });
            next.whenComplete((_, failure) -> {
                updateTails.remove(uuid, next);
                if (failure != null) result.completeExceptionally(failure);
            });
            return next;
        });
        return result;
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
        user.timestamps.firstJoin = System.currentTimeMillis();
        return user;
    }

    private CellUser copy(CellUser source) {
        var copy = new CellUser(source.uuid);
        copy.lastKnownName = source.lastKnownName;

        copy.timestamps.firstJoin = source.timestamps.firstJoin;
        copy.timestamps.lastJoin = source.timestamps.lastJoin;
        copy.timestamps.lastQuit = source.timestamps.lastQuit;
        copy.timestamps.playTimeMillis = source.timestamps.playTimeMillis;
        copy.timestamps.lastActivityAt = source.timestamps.lastActivityAt;
        copy.timestamps.activeSessionStartedAt = source.timestamps.activeSessionStartedAt;

        copy.state.afk = source.state.afk;
        copy.state.god = source.state.god;
        copy.state.flying = source.state.flying;
        copy.state.vanished = source.state.vanished;
        copy.state.nickname = source.state.nickname;
        copy.state.personalTime = source.state.personalTime;
        copy.state.personalWeather = source.state.personalWeather;
        source.state.powerToolCommands.forEach((item, commands) ->
                copy.state.powerToolCommands.put(item, new ArrayList<>(commands))
        );
        copy.state.unlimitedItems.addAll(source.state.unlimitedItems);

        copy.preferences.privateMessages = source.preferences.privateMessages;
        copy.preferences.payments = source.preferences.payments;
        copy.preferences.teleportRequests = source.preferences.teleportRequests;
        copy.preferences.teleportAutoAccept = source.preferences.teleportAutoAccept;
        copy.preferences.confirmLargePayments = source.preferences.confirmLargePayments;
        copy.preferences.confirmInventoryClears = source.preferences.confirmInventoryClears;
        copy.preferences.replyToLastRecipient = source.preferences.replyToLastRecipient;
        copy.preferences.powerToolsEnabled = source.preferences.powerToolsEnabled;
        copy.preferences.socialSpy = source.preferences.socialSpy;
        copy.preferences.incomingReplyTarget = source.preferences.incomingReplyTarget;
        copy.preferences.outgoingReplyTarget = source.preferences.outgoingReplyTarget;

        copy.relations.ignored.addAll(source.relations.ignored);
        copy.cooldowns.putAll(source.cooldowns);
        return copy;
    }

}
