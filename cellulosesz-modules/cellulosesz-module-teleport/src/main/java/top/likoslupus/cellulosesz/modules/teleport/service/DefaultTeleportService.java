package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.teleport.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DefaultTeleportService implements TeleportService {

    private final PlatformService platform;
    private final Scheduler scheduler;
    private final BackLocationService backLocations;
    private final SafeLocationFinder safeLocations;

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

        Runnable action = () -> {
            if (options.rememberBack()) {
                backLocations.remember(player);
            }
            platform.teleport(player, destination)
                    .whenComplete((success, throwable) -> {
                        if (throwable != null) {
                            future.complete(TeleportResult.failed(
                                    "service.teleport.exception",
                                    java.util.Map.of("reason", String.valueOf(throwable.getMessage())),
                                    destination
                            ));
                        } else if (Boolean.TRUE.equals(success)) {
                            future.complete(TeleportResult.success(destination));
                        } else {
                            future.complete(TeleportResult.failed("service.teleport.failed", destination));
                        }
                    });
        };

        if (options.warmupSeconds() <= 0) {
            scheduler.sync(action);
        } else {
            scheduler.syncLater(action, options.warmupSeconds() * 20L);
        }
        return future;
    }

    @Override
    public void rememberBackLocation(CellPlayer player) {
        backLocations.remember(player);
    }

    @Override
    public Optional<CellLocation> backLocation(UUID uuid) {
        return backLocations.location(uuid);
    }

}
