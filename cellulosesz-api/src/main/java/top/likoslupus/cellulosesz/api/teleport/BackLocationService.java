package top.likoslupus.cellulosesz.api.teleport;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BackLocationService {

    CompletableFuture<Void> remember(CellPlayer player);

    CompletableFuture<Void> remember(UUID uuid, CellLocation location);

    CompletableFuture<Void> forget(UUID uuid);

    Optional<CellLocation> location(UUID uuid);

}
