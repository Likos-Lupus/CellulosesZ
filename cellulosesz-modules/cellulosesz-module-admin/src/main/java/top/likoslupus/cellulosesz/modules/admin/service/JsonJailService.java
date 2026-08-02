package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;
import top.likoslupus.cellulosesz.modules.admin.data.JailDocument;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public final class JsonJailService implements JailService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_MUTATIONS = 4_096;

    private final StorageService storage;
    private final Path path;
    private final PlayerDirectory players;
    private final PlayerLocationPlatformService locations;
    private final TeleportService teleports;
    private final ServerThreadExecutor serverThread;
    private final Clock clock;
    private final AdminRuntimeSettings config;

    private final SerialAsyncQueue mutations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_MUTATIONS
    );
    private JailDocument document = new JailDocument();

    public JsonJailService(
            StorageService storage,
            Path path,
            PlayerDirectory players,
            PlayerLocationPlatformService locations,
            TeleportService teleports,
            ServerThreadExecutor serverThread,
            Clock clock,
            AdminRuntimeSettings config
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

    private static JailDocument.JailedEntry toDocument(JailedPlayer value) {
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
        return storage
                .createIfMissing(
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

    private static List<Jail> snapshotJails(JailDocument source) {
        var result = source.jails
                .stream()
                .map(JsonJailService::fromDocument)
                .collect(Collectors.toCollection(ArrayList::new));
        return List.copyOf(result);
    }

    private static List<JailedPlayer> snapshotJailed(JailDocument source) {
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

    private static Jail fromDocument(JailDocument.JailEntry value) {
        return new Jail(
                validateName(value.name),
                copy(value.location),
                requireNonNull(value.createdBy, "createdBy"),
                Instant.ofEpochMilli(value.createdAt)
        );
    }

    private static JailedPlayer fromDocument(JailDocument.JailedEntry value) {
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
                || !value.matches("[a-z0-9_-]+")
        ) {
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
        return mutations
                .submit(() -> {
                    var normalized = validateName(name);
                    var value = new Jail(
                            normalized,
                            location,
                            actor.name(),
                            clock.instant()
                    );

                    return mutateAccepted(current -> {
                        current.jails.removeIf(
                                entry -> entry.name.equalsIgnoreCase(normalized)
                        );

                        current.jails.add(toDocument(value));

                        return AdminResult.success(
                                "service.admin.jail-set",
                                Map.of("jail", normalized)
                        );
                    });
                });
    }

    @Override
    public CompletableFuture<AdminResult> deleteJail(String name) {
        return mutations.submit(() -> {
            var normalized = validateName(name);

            return mutateAccepted(current -> {
                if (current.jailed.stream()
                        .anyMatch(entry ->
                                entry.state.equals(JailState.ACTIVE.name())
                                        && entry.jail.equalsIgnoreCase(normalized)
                        )
                ) {
                    return AdminResult.failure(
                            AdminStatus.INVALID_INPUT,
                            "service.admin.jail-in-use",
                            Map.of("jail", normalized)
                    );
                }

                return current.jails.removeIf(
                        entry -> entry.name.equalsIgnoreCase(normalized)
                )
                        ?
                        AdminResult.success(
                                "service.admin.jail-deleted",
                                Map.of("jail", normalized)
                        )
                        : AdminResult.failure(
                                AdminStatus.NOT_FOUND,
                                "service.admin.jail-not-found",
                                Map.of("jail", normalized)
                        );
            });
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
        return mutations.submit(() -> jailPlayerAccepted(
                player,
                jailName,
                actor,
                expiration,
                reason
        ));
    }

    @Override
    public CompletableFuture<AdminResult> unjail(
            UUID uuid,
            String name,
            AdminActor actor
    ) {
        return mutations.submit(() -> unjailAccepted(uuid, name));
    }

    @Override
    public CompletableFuture<AdminResult> completePendingRelease(CellPlayer player) {
        return mutations.submit(() -> {
            var record = jailedAccepted(player.uuid());

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

            return finishReleaseAccepted(
                    player,
                    record.orElseThrow()
            );
        });
    }

    @Override
    public synchronized Optional<JailedPlayer> jailed(UUID uuid) {
        return snapshotJailed(document).stream()
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
        return mutations.submit(() -> {
            var expired = jailedPlayersIncludingExpired()
                    .stream()
                    .filter(value -> value.expired(clock.instant()))
                    .toList();

            var chain = CompletableFuture.completedFuture(0);

            for (var value : expired) {
                chain = chain.thenCompose(count ->
                        unjailAccepted(
                                value.uuid(),
                                value.name()
                        ).thenApply(result ->
                                result.success()
                                        ? count + 1
                                        : count
                        )
                );
            }

            return chain;
        });
    }

    private Optional<JailedPlayer> jailedAccepted(UUID uuid) {
        synchronized (this) {
            return snapshotJailed(document)
                    .stream()
                    .filter(value -> value.uuid().equals(uuid))
                    .findFirst();
        }
    }

    private static CompletableFuture<AdminResult> completed(AdminResult value) {
        return CompletableFuture.completedFuture(value);
    }

    private CompletableFuture<AdminResult> finishReleaseAccepted(
            CellPlayer player,
            JailedPlayer record
    ) {
        if (record.returnLocation().isEmpty()
                || !config.teleportOnJailRelease()
        ) {
            return removeAccepted(record.uuid())
                    .thenApply(removed ->
                            removed
                                    ?
                                    AdminResult.success(
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

        return teleports
                .teleport(
                        player,
                        record.returnLocation().orElseThrow(),
                        TeleportOptions.defaults().withoutBackMemory()
                )
                .thenCompose(result ->
                        !result.success()
                                ?
                                completed(
                                        AdminResult.failure(
                                                AdminStatus.PLATFORM_FAILURE,
                                                "service.admin.unjail-teleport-failed",
                                                Map.of(
                                                        "player",
                                                        player.name()
                                                )
                                        )
                                )
                                : removeAccepted(record.uuid())
                                        .thenApply(removed ->
                                                removed
                                                        ?
                                                        AdminResult.success(
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

    private CompletableFuture<Boolean> removeAccepted(UUID uuid) {
        return persistAccepted(current -> new Mutation<>(
                current,
                current.jailed.removeIf(value -> value.uuid.equals(uuid.toString()))
        )).exceptionally(_ -> false);
    }

    private CompletableFuture<AdminResult> mutateAccepted(
            Function<JailDocument, AdminResult> operation
    ) {
        return persistAccepted(current -> new Mutation<>(
                current,
                operation.apply(current)
        )).exceptionally(_ -> AdminResult.failure(
                AdminStatus.PERSISTENCE_FAILURE,
                "service.admin.persistence-failed"
        ));
    }

    private static JailDocument.JailEntry toDocument(Jail value) {
        var target = new JailDocument.JailEntry();

        target.name = value.name();
        target.location = value.location();
        target.createdBy = value.createdBy();
        target.createdAt = value.createdAt().toEpochMilli();

        return target;
    }

    private <T> CompletableFuture<T> persistAccepted(
            Function<JailDocument, Mutation<T>> operation
    ) {
        JailDocument current;
        synchronized (this) {
            current = copy(document);
        }

        var mutation = operation.apply(current);
        return storage.save(path, mutation.document()).thenApply(_ -> {
            synchronized (this) {
                document = mutation.document();
            }

            return mutation.result();
        });
    }

    private CompletableFuture<AdminResult> jailPlayerAccepted(
            CellPlayer player,
            String jailName,
            AdminActor actor,
            Expiration expiration,
            String reason
    ) {
        var destination = jailAccepted(jailName);

        if (destination.isEmpty()) {
            return completed(
                    AdminResult.failure(
                            AdminStatus.NOT_FOUND,
                            "service.admin.jail-not-found",
                            Map.of("jail", jailName)
                    )
            );
        }

        var target = destination.orElseThrow();
        return serverThread
                .submit(() -> locations.currentLocation(player))
                .thenCompose(returnLocation -> {
                    var previous = jailedAccepted(player.uuid());
                    var value = new JailedPlayer(
                            player.uuid(),
                            player.name(),
                            target.name(),
                            reason,
                            actor.name(),
                            clock.instant(),
                            expiration,
                            Optional.of(returnLocation),
                            JailState.ACTIVE
                    );

                    return replaceAccepted(value)
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
                                                target.location(),
                                                TeleportOptions.defaults().withoutBackMemory()
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
                                                                        target.name()
                                                                )
                                                        )
                                                );
                                            }

                                            return restoreAccepted(
                                                    player.uuid(),
                                                    previous
                                            ).thenApply(rolledBack ->
                                                    rolledBack
                                                            ?
                                                            AdminResult.failure(
                                                                    AdminStatus.PLATFORM_FAILURE,
                                                                    "service.admin.jail-teleport-failed",
                                                                    Map.of("player", player.name())
                                                            )
                                                            : AdminResult.failure(
                                                                    AdminStatus.ROLLBACK_FAILURE,
                                                                    "service.admin.jail-rollback-failed",
                                                                    Map.of("player", player.name())
                                                            )
                                            );
                                        });
                            });
                });
    }

    private CompletableFuture<AdminResult> unjailAccepted(
            UUID uuid,
            String name
    ) {
        var existing = jailedAccepted(uuid);

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

        return replaceAccepted(pending)
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

                    return finishReleaseAccepted(
                            online.orElseThrow(),
                            pending
                    );
                });
    }

    private synchronized List<JailedPlayer>
    jailedPlayersIncludingExpired() {
        return snapshotJailed(document);
    }

    private Optional<Jail> jailAccepted(String name) {
        var normalized = validateName(name);
        synchronized (this) {
            return snapshotJails(document)
                    .stream()
                    .filter(value -> value.name().equalsIgnoreCase(normalized))
                    .findFirst();
        }
    }

    private CompletableFuture<Boolean> replaceAccepted(JailedPlayer record) {
        return persistAccepted(current -> {
            current.jailed.removeIf(value -> value.uuid.equals(record.uuid().toString()));
            current.jailed.add(toDocument(record));
            return new Mutation<>(current, true);
        }).exceptionally(_ -> false);
    }

    private CompletableFuture<Boolean> restoreAccepted(
            UUID uuid,
            Optional<JailedPlayer> previous
    ) {
        return persistAccepted(current -> {
            current.jailed.removeIf(value -> value.uuid.equals(uuid.toString()));
            previous.ifPresent(value -> current.jailed.add(toDocument(value)));
            return new Mutation<>(current, true);
        }).exceptionally(_ -> false);
    }

    @Override
    public void stopAccepting() {
        mutations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return mutations.drain();
    }

    private record Mutation<T>(
            JailDocument document,
            T result
    ) {

    }

}
