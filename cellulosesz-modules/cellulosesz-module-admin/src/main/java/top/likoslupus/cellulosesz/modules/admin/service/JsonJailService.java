package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.data.JailDocument;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public final class JsonJailService implements JailService, AsyncInitializable {

    private final StorageService storage;
    private final Path path;
    private final PlayerDirectory players;
    private final PlayerLocationPlatformService locations;
    private final TeleportService teleports;
    private final ServerThreadExecutor serverThread;
    private final Clock clock;
    private final AdminConfig config;

    private JailDocument document = new JailDocument();
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonJailService(
            StorageService storage,
            Path path,
            PlayerDirectory players,
            PlayerLocationPlatformService locations,
            TeleportService teleports,
            ServerThreadExecutor serverThread,
            Clock clock,
            AdminConfig config
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.path = requireNonNull(path, "path");
        this.players = requireNonNull(players, "players");
        this.locations = requireNonNull(locations, "locations");
        this.teleports = requireNonNull(teleports, "teleports");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.clock = requireNonNull(clock, "clock");
        this.config = requireNonNull(config, "config");
    }

    private static JailedPlayer withState(
            JailedPlayer value,
            JailState state
    ) {
        return new JailedPlayer(
                value.uuid(),
                value.name(),
                value.jail(),
                value.reason(),
                value.actor(),
                value.createdAt(),
                value.expiration(),
                value.returnLocation(),
                state
        );
    }

    private static JailDocument.JailedEntry toDocument(
            JailedPlayer value
    ) {
        var target = new JailDocument.JailedEntry();

        target.uuid = value.uuid().toString();
        target.name = value.name();
        target.jail = value.jail();
        target.reason = value.reason();
        target.actorName = value.actor();
        target.createdAt = value.createdAt().toEpochMilli();
        target.permanent = value.expiration() instanceof Expiration.Permanent;
        target.expiresAt = value.expiration()
                .expiresAt()
                .map(Instant::toEpochMilli)
                .orElse(0L);
        target.hasReturnLocation =
                value.returnLocation().isPresent();
        value.returnLocation().ifPresent(
                location -> target.returnLocation = location
        );
        target.state = value.state().name();

        return target;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(
                        path,
                        JailDocument.class,
                        JailDocument::new
                )
                .thenApply(loaded -> {
                    snapshotJails(loaded);
                    snapshotJailed(loaded);
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = copy(loaded);
                    }
                });
    }

    private static List<Jail> snapshotJails(
            JailDocument source
    ) {
        var result = source.jails
                .stream()
                .map(JsonJailService::fromDocument)
                .collect(Collectors.toCollection(ArrayList::new));

        return List.copyOf(result);
    }

    private static List<JailedPlayer> snapshotJailed(
            JailDocument source
    ) {
        var result = source.jailed
                .stream()
                .map(JsonJailService::fromDocument)
                .collect(Collectors.toCollection(ArrayList::new));

        return List.copyOf(result);
    }

    private static JailDocument copy(JailDocument source) {
        var target = new JailDocument();

        source.jails.forEach(value -> {
            var next = new JailDocument.JailEntry();

            next.name = value.name;
            next.location = copy(value.location);
            next.createdBy = value.createdBy;
            next.createdAt = value.createdAt;

            target.jails.add(next);
        });

        source.jailed.forEach(value -> {
            var next = new JailDocument.JailedEntry();

            next.uuid = value.uuid;
            next.name = value.name;
            next.jail = value.jail;
            next.reason = value.reason;
            next.actorUuid = value.actorUuid;
            next.actorName = value.actorName;
            next.createdAt = value.createdAt;
            next.permanent = value.permanent;
            next.expiresAt = value.expiresAt;
            next.returnLocation = copy(value.returnLocation);
            next.hasReturnLocation = value.hasReturnLocation;
            next.state = value.state;

            target.jailed.add(next);
        });

        return target;
    }

    private static Jail fromDocument(
            JailDocument.JailEntry value
    ) {
        return new Jail(
                validateName(value.name),
                copy(value.location),
                requireNonNull(value.createdBy, "createdBy"),
                Instant.ofEpochMilli(value.createdAt)
        );
    }

    private static JailedPlayer fromDocument(
            JailDocument.JailedEntry value
    ) {
        var expiration = value.permanent
                ? Expiration.permanent()
                : Expiration.at(
                        Instant.ofEpochMilli(value.expiresAt)
                );

        var returnLocation = value.hasReturnLocation
                ? Optional.of(copy(value.returnLocation))
                : Optional.<CellLocation>empty();

        return new JailedPlayer(
                UUID.fromString(value.uuid),
                value.name,
                validateName(value.jail),
                value.reason,
                value.actorName,
                Instant.ofEpochMilli(value.createdAt),
                expiration,
                returnLocation,
                JailState.valueOf(value.state)
        );
    }

    private static CellLocation copy(CellLocation value) {
        return new CellLocation(
                value.world,
                value.x,
                value.y,
                value.z,
                value.yaw,
                value.pitch
        );
    }

    private static String validateName(String input) {
        var value = requireNonNull(input, "name")
                .trim()
                .toLowerCase(Locale.ROOT);

        if (value.isBlank()
                || value.length() > 32
                || !value.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "invalid jail name"
            );
        }

        return value;
    }

    @Override
    public CompletableFuture<AdminResult> setJail(
            String name,
            CellLocation location,
            AdminActor actor
    ) {
        var normalized = validateName(name);

        var value = new Jail(
                normalized,
                location,
                actor.name(),
                clock.instant()
        );

        return mutate(current -> {
            current.jails.removeIf(
                    entry -> entry.name.equalsIgnoreCase(normalized)
            );

            current.jails.add(toDocument(value));

            return AdminResult.success(
                    "service.admin.jail-set",
                    Map.of("jail", normalized)
            );
        });
    }

    @Override
    public CompletableFuture<AdminResult> deleteJail(
            String name
    ) {
        var normalized = validateName(name);

        return mutate(current -> {
            if (current.jailed.stream()
                    .anyMatch(entry ->
                            entry.state.equals(
                                    JailState.ACTIVE.name()
                            )
                                    && entry.jail.equalsIgnoreCase(
                                    normalized
                            )
                    )) {
                return AdminResult.failure(
                        AdminStatus.INVALID_INPUT,
                        "service.admin.jail-in-use",
                        Map.of("jail", normalized)
                );
            }

            return current.jails.removeIf(
                    entry -> entry.name.equalsIgnoreCase(normalized)
            )
                    ? AdminResult.success(
                    "service.admin.jail-deleted",
                    Map.of("jail", normalized)
            )
                    : AdminResult.failure(
                            AdminStatus.NOT_FOUND,
                            "service.admin.jail-not-found",
                            Map.of("jail", normalized)
                    );
        });
    }

    @Override
    public synchronized Optional<Jail> jail(String name) {
        var normalized = validateName(name);

        return snapshotJails(document)
                .stream()
                .filter(value ->
                        value.name().equalsIgnoreCase(normalized)
                )
                .findFirst();
    }

    @Override
    public synchronized List<Jail> jails() {
        return snapshotJails(document)
                .stream()
                .sorted(Comparator.comparing(
                        Jail::name,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }

    @Override
    public CompletableFuture<AdminResult> jailPlayer(
            CellPlayer player,
            String jailName,
            AdminActor actor,
            Expiration expiration,
            String reason
    ) {
        var destination = jail(jailName);

        if (destination.isEmpty()) {
            return completed(
                    AdminResult.failure(
                            AdminStatus.NOT_FOUND,
                            "service.admin.jail-not-found",
                            Map.of("jail", jailName)
                    )
            );
        }

        return serverThread
                .submit(() -> locations.currentLocation(player))
                .thenCompose(returnLocation -> {
                    var previous = jailed(player.uuid());

                    var value = new JailedPlayer(
                            player.uuid(),
                            player.name(),
                            destination.orElseThrow().name(),
                            reason,
                            actor.name(),
                            clock.instant(),
                            expiration,
                            Optional.of(returnLocation),
                            JailState.ACTIVE
                    );

                    return replace(value)
                            .thenCompose(saved -> {
                                if (!saved) {
                                    return completed(
                                            AdminResult.failure(
                                                    AdminStatus.PERSISTENCE_FAILURE,
                                                    "service.admin.persistence-failed"
                                            )
                                    );
                                }

                                return teleports.teleport(
                                                player,
                                                destination.orElseThrow()
                                                        .location(),
                                                TeleportOptions.defaults()
                                                        .withoutBackMemory()
                                        )
                                        .thenCompose(result -> {
                                            if (result.success()) {
                                                return completed(
                                                        AdminResult.success(
                                                                "service.admin.player-jailed",
                                                                Map.of(
                                                                        "player",
                                                                        player.name(),
                                                                        "jail",
                                                                        destination
                                                                                .orElseThrow()
                                                                                .name()
                                                                )
                                                        )
                                                );
                                            }

                                            return restore(
                                                    player.uuid(),
                                                    previous
                                            ).thenApply(rolledBack ->
                                                    rolledBack
                                                            ? AdminResult.failure(
                                                            AdminStatus.PLATFORM_FAILURE,
                                                            "service.admin.jail-teleport-failed",
                                                            Map.of(
                                                                    "player",
                                                                    player.name()
                                                            )
                                                    )
                                                            : AdminResult.failure(
                                                                    AdminStatus.ROLLBACK_FAILURE,
                                                                    "service.admin.jail-rollback-failed",
                                                                    Map.of(
                                                                            "player",
                                                                            player.name()
                                                                    )
                                                            )
                                            );
                                        });
                            });
                });
    }

    @Override
    public CompletableFuture<AdminResult> unjail(
            UUID uuid,
            String name,
            AdminActor actor
    ) {
        var existing = jailed(uuid);

        if (existing.isEmpty()) {
            return completed(
                    AdminResult.failure(
                            AdminStatus.NOT_FOUND,
                            "service.admin.player-not-jailed",
                            Map.of("player", name)
                    )
            );
        }

        var pending = withState(
                existing.orElseThrow(),
                JailState.RELEASE_PENDING
        );

        return replace(pending)
                .thenCompose(saved -> {
                    if (!saved) {
                        return completed(
                                AdminResult.failure(
                                        AdminStatus.PERSISTENCE_FAILURE,
                                        "service.admin.persistence-failed"
                                )
                        );
                    }

                    var online = players.onlinePlayer(uuid);

                    if (online.isEmpty()) {
                        return completed(
                                AdminResult.success(
                                        "service.admin.unjail-pending",
                                        Map.of("player", name)
                                )
                        );
                    }

                    return finishRelease(
                            online.orElseThrow(),
                            pending
                    );
                });
    }

    @Override
    public CompletableFuture<AdminResult> completePendingRelease(
            CellPlayer player
    ) {
        var record = jailed(player.uuid());

        if (record.isEmpty()
                || record.orElseThrow().state()
                != JailState.RELEASE_PENDING
        ) {
            return completed(
                    AdminResult.failure(
                            AdminStatus.NOT_FOUND,
                            "service.admin.player-not-jailed",
                            Map.of("player", player.name())
                    )
            );
        }

        return finishRelease(
                player,
                record.orElseThrow()
        );
    }

    @Override
    public synchronized Optional<JailedPlayer> jailed(
            UUID uuid
    ) {
        return snapshotJailed(document)
                .stream()
                .filter(value -> value.uuid().equals(uuid))
                .findFirst();
    }

    @Override
    public synchronized List<JailedPlayer> jailedPlayers() {
        var now = clock.instant();

        return snapshotJailed(document)
                .stream()
                .filter(value -> !value.expired(now))
                .sorted(Comparator.comparing(
                        JailedPlayer::name,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }

    @Override
    public CompletableFuture<Integer> purgeExpired() {
        var expired = jailedPlayersIncludingExpired()
                .stream()
                .filter(value -> value.expired(clock.instant()))
                .toList();

        var chain = CompletableFuture.completedFuture(0);

        for (var value : expired) {
            chain = chain.thenCompose(count ->
                    unjail(
                            value.uuid(),
                            value.name(),
                            AdminActor.console("system")
                    ).thenApply(result ->
                            result.success()
                                    ? count + 1
                                    : count
                    )
            );
        }

        return chain;
    }

    private static CompletableFuture<AdminResult> completed(
            AdminResult value
    ) {
        return CompletableFuture.completedFuture(value);
    }

    private CompletableFuture<AdminResult> finishRelease(
            CellPlayer player,
            JailedPlayer record
    ) {
        if (record.returnLocation().isEmpty()
                || !config.teleportOnJailRelease
        ) {
            return remove(record.uuid())
                    .thenApply(removed ->
                            removed
                                    ? AdminResult.success(
                                    "service.admin.player-unjailed",
                                    Map.of(
                                            "player",
                                            player.name()
                                    )
                            )
                                    : AdminResult.failure(
                                            AdminStatus.PERSISTENCE_FAILURE,
                                            "service.admin.persistence-failed"
                                    )
                    );
        }

        return teleports.teleport(
                        player,
                        record.returnLocation().orElseThrow(),
                        TeleportOptions.defaults()
                                .withoutBackMemory()
                )
                .thenCompose(result ->
                        !result.success()
                                ? completed(
                                AdminResult.failure(
                                        AdminStatus.PLATFORM_FAILURE,
                                        "service.admin.unjail-teleport-failed",
                                        Map.of(
                                                "player",
                                                player.name()
                                        )
                                )
                        )
                                : remove(record.uuid())
                                        .thenApply(removed ->
                                                removed
                                                        ? AdminResult.success(
                                                        "service.admin.player-unjailed",
                                                        Map.of(
                                                                "player",
                                                                player.name()
                                                        )
                                                )
                                                        : AdminResult.failure(
                                                                AdminStatus.ROLLBACK_FAILURE,
                                                                "service.admin.unjail-remove-failed",
                                                                Map.of(
                                                                        "player",
                                                                        player.name()
                                                                )
                                                        )
                                        )
                );
    }

    private CompletableFuture<Boolean> remove(UUID uuid) {
        var result = new CompletableFuture<Boolean>();

        enqueue(
                current -> new Mutation<>(
                        current,
                        current.jailed.removeIf(
                                value -> value.uuid.equals(uuid.toString())
                        )
                ),
                result
        );

        return result;
    }

    private CompletableFuture<AdminResult> mutate(
            Function<JailDocument, AdminResult> operation
    ) {
        var result = new CompletableFuture<AdminResult>();

        enqueue(
                current -> new Mutation<>(
                        current,
                        operation.apply(current)
                ),
                result
        );

        return result;
    }

    private static JailDocument.JailEntry toDocument(
            Jail value
    ) {
        var target = new JailDocument.JailEntry();

        target.name = value.name();
        target.location = value.location();
        target.createdBy = value.createdBy();
        target.createdAt = value.createdAt().toEpochMilli();

        return target;
    }

    private synchronized <T> void enqueue(
            Function<JailDocument, Mutation<T>> operation,
            CompletableFuture<T> result
    ) {
        mutationTail = mutationTail
                .handle((_, _) -> null)
                .thenCompose(_ -> {
                    JailDocument current;

                    synchronized (this) {
                        current = copy(document);
                    }

                    final Mutation<T> mutation;

                    try {
                        mutation = operation.apply(current);
                    } catch (RuntimeException failure) {
                        result.completeExceptionally(failure);
                        return CompletableFuture.completedFuture(null);
                    }

                    return storage.save(
                                    path,
                                    mutation.document()
                            )
                            .handle((_, failure) -> {
                                if (failure == null) {
                                    synchronized (this) {
                                        document = mutation.document();
                                    }

                                    result.complete(mutation.result());
                                } else if (mutation.result() instanceof AdminResult) {
                                    @SuppressWarnings("unchecked")
                                    var value = (T) AdminResult.failure(
                                            AdminStatus.PERSISTENCE_FAILURE,
                                            "service.admin.persistence-failed"
                                    );

                                    result.complete(value);
                                } else {
                                    result.completeExceptionally(failure);
                                }

                                return (Void) null;
                            });
                });

        mutationTail.whenComplete((_, failure) -> {
            if (failure != null) {
                result.completeExceptionally(failure);
            }
        });
    }

    private synchronized List<JailedPlayer>
    jailedPlayersIncludingExpired() {
        return snapshotJailed(document);
    }

    private CompletableFuture<Boolean> replace(
            JailedPlayer record
    ) {
        var result = new CompletableFuture<Boolean>();

        enqueue(
                current -> {
                    current.jailed.removeIf(value ->
                            value.uuid.equals(
                                    record.uuid().toString()
                            )
                    );

                    current.jailed.add(toDocument(record));

                    return new Mutation<>(current, true);
                },
                result
        );

        return result;
    }

    private CompletableFuture<Boolean> restore(
            UUID uuid,
            Optional<JailedPlayer> previous
    ) {
        var result = new CompletableFuture<Boolean>();

        enqueue(
                current -> {
                    current.jailed.removeIf(value ->
                            value.uuid.equals(uuid.toString())
                    );

                    previous.ifPresent(value ->
                            current.jailed.add(toDocument(value))
                    );

                    return new Mutation<>(current, true);
                },
                result
        );

        return result.exceptionally(_ -> false);
    }

    private record Mutation<T>(
            JailDocument document,
            T result
    ) {

    }

}
