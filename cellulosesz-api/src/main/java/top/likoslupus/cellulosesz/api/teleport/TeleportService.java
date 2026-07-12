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

    /**
     * Cancels a pending teleport warmup for the player.
     *
     * @param uuid       player identifier
     * @param messageKey localized failure key completed into the pending result
     *
     * @return whether a pending warmup was cancelled
     */
    boolean cancelWarmup(UUID uuid, String messageKey);

    boolean warmingUp(UUID uuid);

    void rememberBackLocation(CellPlayer player);

    void rememberBackLocation(UUID uuid, CellLocation location);

    Optional<CellLocation> backLocation(UUID uuid);

}
