package top.likoslupus.cellulosesz.api.teleport;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TeleportService {

    CompletableFuture<TeleportResult> teleport(
            CellPlayer player,
            CellLocation target,
            TeleportOptions options
    );

    boolean cancelWarmup(UUID uuid, TeleportStatus status);

    boolean warmingUp(UUID uuid);

    CompletableFuture<Void> rememberBackLocation(CellPlayer player);

    CompletableFuture<Void> rememberBackLocation(UUID uuid, CellLocation location);

    Optional<CellLocation> backLocation(UUID uuid);

    void shutdown();

}
