package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.scheduler.TaskHandle;
import top.likoslupus.cellulosesz.api.teleport.*;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public final class DefaultTeleportService implements TeleportService {

    private final TeleportOperations operations;
    private final PlayerLocationPlatformService locations;
    private final ServerThreadExecutor serverThread;
    private final Scheduler scheduler;
    private final BackLocationService backLocations;
    private final Clock clock;
    private final Map<UUID, PendingWarmup> warmups = new ConcurrentHashMap<>();

    public DefaultTeleportService(
            TeleportOperations operations,
            PlayerLocationPlatformService locations,
            ServerThreadExecutor serverThread,
            Scheduler scheduler,
            BackLocationService backLocations,
            Clock clock
    ) {
        this.operations = requireNonNull(operations, "operations");
        this.locations = requireNonNull(locations, "locations");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.scheduler = requireNonNull(scheduler, "scheduler");
        this.backLocations = requireNonNull(backLocations, "backLocations");
        this.clock = requireNonNull(clock, "clock");
    }

    private static void completeFailure(
            CompletableFuture<TeleportResult> result,
            Throwable failure,
            TeleportStatus status,
            String key
    ) {
        var cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        result.complete(
                TeleportResult.failed(status, key,
                        Map.of("reason", cause.getClass().getSimpleName()))
        );
    }

    private static CellLocation copy(CellLocation value) {
        return new CellLocation(
                value.world,
                value.x, value.y, value.z,
                value.yaw, value.pitch
        );
    }

    @Override
    public CompletableFuture<TeleportResult> teleport(
            CellPlayer player,
            CellLocation target,
            TeleportOptions options
    ) {
        requireNonNull(player, "player");
        requireNonNull(target, "target");
        requireNonNull(options, "options");

        var result = new CompletableFuture<TeleportResult>();
        serverThread
                .submit(() -> prepare(player, target, options))
                .whenComplete((prepared, failure) -> {
                    if (failure != null) {
                        completeFailure(
                                result,
                                failure,
                                TeleportStatus.PLATFORM_FAILURE,
                                "service.teleport.exception"
                        );
                        return;
                    }

                    if (prepared.failure().isPresent()) {
                        result.complete(prepared.failure().orElseThrow());
                        return;
                    }

                    if (options.warmupSeconds() == 0) {
                        execute(
                                player,
                                target,
                                options,
                                result
                        );
                        return;
                    }

                    scheduleWarmup(
                            player,
                            target,
                            options,
                            result
                    );
                });

        return result;
    }

    @Override
    public boolean cancelWarmup(UUID uuid, TeleportStatus status) {
        requireNonNull(status, "status");
        if (!status.name().startsWith("CANCELLED_")) {
            throw new IllegalArgumentException("cancellation status required");
        }

        var pending = warmups.remove(uuid);
        if (pending == null) {
            return false;
        }

        cancelPending(pending, status);
        return true;
    }

    private void cancelPending(PendingWarmup pending, TeleportStatus status) {
        pending.handle().cancel();
        pending.future().complete(TeleportResult.failed(status, cancellationKey(status)));
    }

    private static String cancellationKey(TeleportStatus status) {
        return switch (status) {
            case CANCELLED_MOVE -> "service.teleport.cancelled-move";
            case CANCELLED_DAMAGE -> "service.teleport.cancelled-damage";
            case CANCELLED_DEATH -> "service.teleport.cancelled-death";
            case CANCELLED_DISCONNECT -> "service.teleport.cancelled-disconnect";
            case CANCELLED_REPLACED -> "service.teleport.cancelled-replaced";
            default -> throw new IllegalArgumentException("not a cancellation status: " + status);
        };
    }

    @Override
    public boolean warmingUp(UUID uuid) {
        return warmups.containsKey(uuid);
    }

    @Override
    public CompletableFuture<Void> rememberBackLocation(CellPlayer player) {
        return backLocations.remember(player);
    }

    @Override
    public CompletableFuture<Void> rememberBackLocation(UUID uuid, CellLocation location) {
        return backLocations.remember(uuid, location);
    }

    @Override
    public Optional<CellLocation> backLocation(UUID uuid) {
        return backLocations.location(uuid);
    }

    @Override
    public void shutdown() {
        warmups.forEach((_, pending) ->
                cancelPending(pending, TeleportStatus.CANCELLED_REPLACED)
        );
        warmups.clear();
    }

    private void scheduleWarmup(
            CellPlayer player,
            CellLocation target,
            TeleportOptions options,
            CompletableFuture<TeleportResult> result
    ) {
        final long ticks;
        try {
            ticks = Math.multiplyExact(options.warmupSeconds(), 20L);
        } catch (ArithmeticException failure) {
            result.complete(TeleportResult.failed(
                    TeleportStatus.PLATFORM_FAILURE,
                    "service.teleport.invalid-warmup"
            ));
            return;
        }

        final PendingWarmup[] holder = new PendingWarmup[1];
        var handle = scheduler
                .syncLater(
                        () -> {
                            var pending = holder[0];
                            if (!warmups.remove(player.uuid(), pending) || result.isDone()) {
                                return;
                            }
                            execute(player, target, options, result);
                        },
                        ticks
                );

        var pending = new PendingWarmup(handle, result);
        holder[0] = pending;

        var replaced = warmups.put(player.uuid(), pending);
        if (replaced != null) {
            cancelPending(replaced, TeleportStatus.CANCELLED_REPLACED);
        }
    }

    private void execute(
            CellPlayer player,
            CellLocation requested,
            TeleportOptions options,
            CompletableFuture<TeleportResult> result
    ) {
        serverThread
                .submit(() -> prepare(player, requested, options))
                .whenComplete((prepared, failure) -> {
                    if (failure != null) {
                        completeFailure(
                                result,
                                failure,
                                TeleportStatus.PLATFORM_FAILURE,
                                "service.teleport.exception"
                        );
                        return;
                    }

                    if (prepared.failure().isPresent()) {
                        result.complete(prepared.failure().orElseThrow());
                        return;
                    }

                    var value = prepared.value().orElseThrow();
                    var previousBack = backLocations.location(player.uuid());
                    var precommit = options.rememberBack()
                            ? backLocations.remember(player.uuid(), value.origin())
                            : CompletableFuture.completedFuture(null);

                    precommit
                            .whenComplete((_, persistenceFailure) -> {
                                if (persistenceFailure != null) {
                                    result.complete(TeleportResult.failed(
                                            TeleportStatus.BACK_PERSISTENCE_FAILURE,
                                            "service.teleport.back-persistence-failed"
                                    ));
                                    return;
                                }

                                serverThread
                                        .submit(() -> operations.move(player, value.destination()))
                                        .whenComplete((move, moveFailure) -> {
                                            if (moveFailure == null && move.successful()) {
                                                result.complete(TeleportResult.success(value.destination()));
                                                return;
                                            }

                                            restoreBack(player.uuid(), previousBack, options.rememberBack())
                                                    .whenComplete((_, rollbackFailure) -> {
                                                        if (rollbackFailure != null) {
                                                            result.complete(TeleportResult.failed(
                                                                    TeleportStatus.ROLLBACK_FAILURE,
                                                                    "service.teleport.back-rollback-failed"
                                                            ));
                                                        } else if (moveFailure != null) {
                                                            completeFailure(
                                                                    result,
                                                                    moveFailure,
                                                                    TeleportStatus.PLATFORM_FAILURE,
                                                                    "service.teleport.exception"
                                                            );
                                                        } else {
                                                            result.complete(TeleportResult.failed(
                                                                    TeleportStatus.PLATFORM_FAILURE,
                                                                    "service.teleport.failed"
                                                            ));
                                                        }
                                                    });
                                        });
                            });
                });
    }

    private PreparedResult prepare(
            CellPlayer player,
            CellLocation requested,
            TeleportOptions options
    ) {
        // Accessing the injected clock here makes preparation deterministic under tests and avoids scattered wall clocks.
        clock.instant();
        var origin = locations.currentLocation(player);

        if (!options.allowCrossWorld() && !origin.world.equals(requested.world)) {
            return PreparedResult.failure(TeleportResult.failed(
                    TeleportStatus.CROSS_WORLD_DISABLED,
                    "service.teleport.cross-world-disabled"
            ));
        }

        if (!options.safe()) {
            return PreparedResult.success(new Prepared(copy(origin), copy(requested)));
        }

        var safe = operations.safeLocation(requested);
        if (!safe.successful() || safe.value().isEmpty()) {
            return PreparedResult.failure(TeleportResult.failed(
                    TeleportStatus.UNSAFE_DESTINATION,
                    "service.teleport.unsafe"
            ));
        }

        return PreparedResult.success(new Prepared(
                copy(origin),
                copy(safe.value().orElseThrow())
        ));
    }

    private CompletableFuture<Void> restoreBack(
            UUID uuid,
            Optional<CellLocation> previous,
            boolean changed
    ) {
        return !changed
                ? CompletableFuture.completedFuture(null)
                : previous.isPresent()
                        ? backLocations.remember(uuid, previous.orElseThrow())
                        : backLocations.forget(uuid);
    }

    private record Prepared(
            CellLocation origin,
            CellLocation destination
    ) {

    }

    private record PreparedResult(
            Optional<Prepared> value,
            Optional<TeleportResult> failure
    ) {

        static PreparedResult success(Prepared value) {
            return new PreparedResult(Optional.of(value), Optional.empty());
        }

        static PreparedResult failure(TeleportResult failure) {
            return new PreparedResult(Optional.empty(), Optional.of(failure));
        }

    }

    private record PendingWarmup(
            TaskHandle handle,
            CompletableFuture<TeleportResult> future
    ) {

    }

}
