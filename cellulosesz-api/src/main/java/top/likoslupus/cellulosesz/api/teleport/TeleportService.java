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

    void rememberBackLocation(CellPlayer player);

    Optional<CellLocation> backLocation(UUID uuid);

}
