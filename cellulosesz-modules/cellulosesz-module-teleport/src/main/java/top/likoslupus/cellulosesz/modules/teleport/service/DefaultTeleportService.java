package top.likoslupus.cellulosesz.modules.teleport.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.scheduler.TaskHandle;
import top.likoslupus.cellulosesz.api.teleport.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultTeleportService implements TeleportService {

    private final PlatformService platform;
    private final Scheduler scheduler;
    private final BackLocationService backLocations;
    private final SafeLocationFinder safeLocations;
    private final Map<UUID, PendingWarmup> warmups = new ConcurrentHashMap<>();

    public DefaultTeleportService(
            PlatformService platform,
            Scheduler scheduler,
            BackLocationService backLocations,
            SafeLocationFinder safeLocations
    ) {
        this.platform = platform;
        this.scheduler = scheduler;
        this.backLocations = backLocations;
        this.safeLocations = safeLocations;
    }

    @Override
    public CompletableFuture<TeleportResult> teleport(
            CellPlayer player,
            CellLocation target,
            TeleportOptions options
    ) {
        var future = new CompletableFuture<TeleportResult>();
        var resolvedOptions = ResolvedOptions.from(options);
        var origin = platform.location(player);
        if (!resolvedOptions.allowCrossWorld && !origin.world.equals(target.world)) {
            future.complete(TeleportResult.failed("service.teleport.cross-world-disabled", target));
            return future;
        }

        var destination = resolvedOptions.safe
                ? safeLocations.safeLocation(target)
                : Optional.of(target);
        if (destination.isEmpty()) {
            future.complete(TeleportResult.failed("service.teleport.unsafe", target));
            return future;
        }

        if (resolvedOptions.warmupSeconds <= 0) {
            cancelWarmup(player.uuid(), "service.teleport.cancelled-replaced");
            scheduler.sync(() -> execute(player, destination.get(), resolvedOptions, future));
            return future;
        }

        var pendingRef = new AtomicReference<@Nullable PendingWarmup>();
        Runnable action = () -> {
            var pending = pendingRef.get();
            if (pending == null || !warmups.remove(player.uuid(), pending)) return;
            execute(player, destination.get(), resolvedOptions, future);
        };

        var handle = scheduler.syncLater(action, resolvedOptions.warmupSeconds * 20L);
        var pending = new PendingWarmup(handle, future, destination.get());
        pendingRef.set(pending);
        var replaced = warmups.put(player.uuid(), pending);
        if (replaced != null) {
            cancelPending(replaced, "service.teleport.cancelled-replaced");
        }
        return future;
    }

    private void execute(
            CellPlayer player,
            CellLocation destination,
            ResolvedOptions options,
            CompletableFuture<TeleportResult> future
    ) {
        if (future.isDone()) return;

        var checkedDestination = options.safe
                ? safeLocations.safeLocation(destination)
                : Optional.of(destination);
        if (checkedDestination.isEmpty()) {
            future.complete(TeleportResult.failed("service.teleport.unsafe", destination));
            return;
        }

        var target = checkedDestination.orElseThrow();
        var origin = platform.location(player);
        if (!options.allowCrossWorld && !origin.world.equals(target.world)) {
            future.complete(TeleportResult.failed("service.teleport.cross-world-disabled", target));
            return;
        }

        var previousBack = backLocations.location(player.uuid());
        var precommit = options.rememberBack
                ? backLocations.remember(player.uuid(), origin)
                : CompletableFuture.completedFuture(null);

        precommit.whenComplete((unused, persistenceFailure) -> {
            if (persistenceFailure != null) {
                future.complete(TeleportResult.failed(
                        "service.teleport.back-persistence-failed",
                        target
                ));
                return;
            }
            platform.callOnServerThread(() -> platform.teleport(player, target))
                    .thenCompose(value -> value)
                    .handle((success, failure) -> new PlatformOutcome(Boolean.TRUE.equals(success), failure))
                    .thenCompose(outcome -> {
                        if (outcome.success()) return CompletableFuture.completedFuture(outcome);
                        return restoreBack(player.uuid(), previousBack, options.rememberBack)
                                .handle((rollbackUnused, rollbackFailure) -> outcome.withRollbackFailure(rollbackFailure));
                    })
                    .whenComplete((outcome, chainFailure) -> {
                        if (future.isDone()) return;
                        if (chainFailure != null || outcome.rollbackFailure() != null) {
                            future.complete(TeleportResult.failed(
                                    "service.teleport.back-rollback-failed",
                                    target
                            ));
                        } else if (outcome.success()) {
                            future.complete(TeleportResult.success(target));
                        } else if (outcome.failure() != null) {
                            future.complete(TeleportResult.failed(
                                    "service.teleport.exception",
                                    Map.of("reason", failureMessage(outcome.failure())),
                                    target
                            ));
                        } else {
                            future.complete(TeleportResult.failed("service.teleport.failed", target));
                        }
                    });
        });
    }

    private void cancelPending(PendingWarmup pending, String messageKey) {
        pending.handle.cancel();
        pending.future.complete(TeleportResult.failed(messageKey, pending.destination));
    }

    private CompletableFuture<Void> restoreBack(
            UUID uuid,
            Optional<CellLocation> previous,
            boolean changed
    ) {
        if (!changed) return CompletableFuture.completedFuture(null);
        return previous.isPresent()
                ? backLocations.remember(uuid, previous.orElseThrow())
                : backLocations.forget(uuid);
    }

    private String failureMessage(Throwable failure) {
        var cause = failure;
        while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        var message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    @Override
    public boolean cancelWarmup(UUID uuid, String messageKey) {
        var pending = warmups.remove(uuid);
        if (pending == null) return false;

        cancelPending(pending, messageKey);
        return true;
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

    private record PlatformOutcome(
            boolean success,
            @Nullable Throwable failure,
            @Nullable Throwable rollbackFailure
    ) {

        private PlatformOutcome(boolean success, @Nullable Throwable failure) {
            this(success, failure, null);
        }

        private PlatformOutcome withRollbackFailure(@Nullable Throwable rollbackFailure) {
            return new PlatformOutcome(success, failure, rollbackFailure);
        }

    }

    private record PendingWarmup(
            TaskHandle handle,
            CompletableFuture<TeleportResult> future,
            CellLocation destination
    ) {

    }

    private record ResolvedOptions(
            boolean safe,
            boolean rememberBack,
            boolean allowCrossWorld,
            int warmupSeconds
    ) {

        private static ResolvedOptions from(TeleportOptions options) {
            return new ResolvedOptions(
                    options.safe(),
                    options.rememberBack(),
                    options.allowCrossWorld(),
                    Math.max(0, options.warmupSeconds())
            );
        }

    }

}
