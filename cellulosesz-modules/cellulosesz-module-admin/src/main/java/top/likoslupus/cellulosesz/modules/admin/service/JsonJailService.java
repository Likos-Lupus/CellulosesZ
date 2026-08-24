package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.admin.Expiration;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncCloseable;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncInitializable;
import top.likoslupus.cellulosesz.core.storage.StorageService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;
import top.likoslupus.cellulosesz.modules.admin.domain.*;
import top.likoslupus.cellulosesz.modules.admin.persistence.JailDocument;
import top.likoslupus.cellulosesz.modules.admin.persistence.JailMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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
        return JailMapper.fromDomain(value);
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
                        document = JailMapper.copy(loaded);
                    }
                });
    }

    private static List<Jail> snapshotJails(JailDocument source) {
        return source.jails.stream()
                .map(JailMapper::toDomain)
                .toList();
    }

    private static List<JailedPlayer> snapshotJailed(JailDocument source) {
        return source.jailed.stream()
                .map(JailMapper::toDomain)
                .toList();
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
                                MessageArguments.builder().add(normalized).build()
                        );
                    });
                });
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
        return JailMapper.fromDomain(value);
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

    private static JailDocument copy(JailDocument source) {
        return JailMapper.copy(source);
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
                            MessageArguments.builder().add(normalized).build()
                    );
                }

                return current.jails.removeIf(
                        entry -> entry.name.equalsIgnoreCase(normalized)
                )
                        ?
                        AdminResult.success(
                                "service.admin.jail-deleted",
                                MessageArguments.builder().add(normalized).build()
                        )
                        : AdminResult.failure(
                                AdminStatus.NOT_FOUND,
                                "service.admin.jail-not-found",
                                MessageArguments.builder().add(normalized).build()
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
                                MessageArguments.builder().add(player.name()).build()
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
                    .handle((removed, failure) -> {
                        if (failure != null) {
                            return AdminResult.failure(
                                    AdminStatus.PERSISTENCE_FAILURE,
                                    "service.admin.persistence-failed"
                            );
                        }

                        return removed
                                ?
                                AdminResult.success(
                                        "service.admin.player-unjailed",
                                        MessageArguments.builder().add(player.name()).build()
                                )
                                : AdminResult.failure(
                                        AdminStatus.NOT_FOUND,
                                        "service.admin.player-not-jailed",
                                        MessageArguments.builder().add(player.name()).build()
                                );
                    });
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
                                                MessageArguments.builder()
                                                        .add(player.name())
                                                        .build()
                                        )
                                )
                                : removeAccepted(record.uuid())
                                        .handle((removed, failure) ->
                                                failure == null && removed
                                                        ?
                                                        AdminResult.success(
                                                                "service.admin.player-unjailed",
                                                                MessageArguments.builder()
                                                                        .add(player.name())
                                                                        .build()
                                                        )
                                                        : AdminResult.failure(
                                                                AdminStatus.ROLLBACK_FAILURE,
                                                                "service.admin.unjail-remove-failed",
                                                                MessageArguments.builder()
                                                                        .add(player.name())
                                                                        .build()
                                                        )
                                        )
                );
    }

    private CompletableFuture<Boolean> removeAccepted(UUID uuid) {
        return persistAccepted(current -> new Mutation<>(
                current,
                current.jailed.removeIf(value -> value.uuid.equals(uuid.toString()))
        ));
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
                            MessageArguments.builder().add(jailName).build()
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
                                if (!saved.successful()) {
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
                                                                MessageArguments.builder()
                                                                        .add(
                                                                                player.name()
                                                                        )
                                                                        .add(target.name())
                                                                        .build()
                                                        )
                                                );
                                            }

                                            return restoreAccepted(
                                                    player.uuid(),
                                                    previous
                                            ).thenApply(rolledBack ->
                                                    rolledBack.successful()
                                                            ?
                                                            AdminResult.failure(
                                                                    AdminStatus.PLATFORM_FAILURE,
                                                                    "service.admin.jail-teleport-failed",
                                                                    MessageArguments.builder()
                                                                            .add(
                                                                                    player.name()
                                                                            )
                                                                            .build()
                                                            )
                                                            : AdminResult.failure(
                                                                    AdminStatus.ROLLBACK_FAILURE,
                                                                    "service.admin.jail-rollback-failed",
                                                                    MessageArguments.builder()
                                                                            .add(
                                                                                    player.name()
                                                                            )
                                                                            .build()
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
                            MessageArguments.builder().add(name).build()
                    )
            );
        }

        var pending = withState(
                existing.orElseThrow(),
                JailState.RELEASE_PENDING
        );

        return replaceAccepted(pending)
                .thenCompose(saved -> {
                    if (!saved.successful()) {
                        return completed(
                                AdminResult.failure(
                                        AdminStatus.PERSISTENCE_FAILURE,
                                        "service.admin.persistence-failed"
                                )
                        );
                    }

                    var online = players.onlinePlayer(uuid);

                    if (online == null) {
                        return completed(
                                AdminResult.success(
                                        "service.admin.unjail-pending",
                                        MessageArguments.builder().add(name).build()
                                )
                        );
                    }

                    return finishReleaseAccepted(
                            online,
                            pending
                    );
                });
    }

    private synchronized List<JailedPlayer> jailedPlayersIncludingExpired() {
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

    private CompletableFuture<PlatformResult<Void>> replaceAccepted(JailedPlayer record) {
        return persistAccepted(current -> {
            current.jailed.removeIf(value -> value.uuid.equals(record.uuid().toString()));
            current.jailed.add(toDocument(record));
            return new Mutation<>(current, true);
        })
                .thenApply(_ -> PlatformResult.success())
                .exceptionally(failure -> PlatformResult.failure(
                        PlatformOperationStatus.STORAGE_FAILURE,
                        failure.getMessage() == null
                                ? "Failed to persist jailed player"
                                : failure.getMessage()
                ));
    }

    private CompletableFuture<PlatformResult<Void>> restoreAccepted(
            UUID uuid,
            Optional<JailedPlayer> previous
    ) {
        return persistAccepted(current -> {
            current.jailed.removeIf(value -> value.uuid.equals(uuid.toString()));
            previous.ifPresent(value -> current.jailed.add(toDocument(value)));
            return new Mutation<>(current, true);
        })
                .thenApply(_ -> PlatformResult.success())
                .exceptionally(failure -> PlatformResult.failure(
                        PlatformOperationStatus.STORAGE_FAILURE,
                        failure.getMessage() == null
                                ? "Failed to restore jailed player"
                                : failure.getMessage()
                ));
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
