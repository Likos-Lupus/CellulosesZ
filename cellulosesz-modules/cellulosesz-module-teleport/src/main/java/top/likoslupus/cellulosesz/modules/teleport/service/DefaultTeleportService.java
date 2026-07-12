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
        var destination = options.safe()
                ? safeLocations.safeLocation(target).orElse(target)
                : target;

        cancelWarmup(player.uuid(), "service.teleport.cancelled-replaced");

        if (options.warmupSeconds() <= 0) {
            scheduler.sync(() -> execute(player, destination, options, future));
            return future;
        }

        var pendingRef = new AtomicReference<@Nullable PendingWarmup>();
        Runnable action = () -> {
            var pending = pendingRef.get();
            if (pending == null || !warmups.remove(player.uuid(), pending)) return;
            execute(player, destination, options, future);
        };

        var handle = scheduler.syncLater(action, options.warmupSeconds() * 20L);
        var pending = new PendingWarmup(handle, future, destination);
        pendingRef.set(pending);
        warmups.put(player.uuid(), pending);
        return future;
    }

    private void execute(
            CellPlayer player,
            CellLocation destination,
            TeleportOptions options,
            CompletableFuture<TeleportResult> future
    ) {
        if (future.isDone()) return;
        if (options.rememberBack()) {
            backLocations.remember(player);
        }
        platform.teleport(player, destination)
                .whenComplete((success, throwable) -> {
                    if (throwable != null) {
                        future.complete(TeleportResult.failed(
                                "service.teleport.exception",
                                Map.of("reason", String.valueOf(throwable.getMessage())),
                                destination
                        ));
                    } else if (Boolean.TRUE.equals(success)) {
                        future.complete(TeleportResult.success(destination));
                    } else {
                        future.complete(TeleportResult.failed("service.teleport.failed", destination));
                    }
                });
    }

    @Override
    public boolean cancelWarmup(UUID uuid, String messageKey) {
        var pending = warmups.remove(uuid);
        if (pending == null) return false;

        pending.handle.cancel();
        pending.future.complete(TeleportResult.failed(messageKey, pending.destination));
        return true;
    }

    @Override
    public boolean warmingUp(UUID uuid) {
        return warmups.containsKey(uuid);
    }

    @Override
    public void rememberBackLocation(CellPlayer player) {
        backLocations.remember(player);
    }

    @Override
    public void rememberBackLocation(UUID uuid, CellLocation location) {
        backLocations.remember(uuid, location);
    }

    @Override
    public Optional<CellLocation> backLocation(UUID uuid) {
        return backLocations.location(uuid);
    }

    private record PendingWarmup(
            TaskHandle handle,
            CompletableFuture<TeleportResult> future,
            CellLocation destination
    ) {

    }

}
