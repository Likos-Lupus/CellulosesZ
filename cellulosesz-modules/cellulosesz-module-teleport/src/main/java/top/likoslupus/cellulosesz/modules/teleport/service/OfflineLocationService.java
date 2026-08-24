package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface OfflineLocationService {

    CompletableFuture<Void> remember(UUID uuid, CellLocation location);

    Optional<CellLocation> location(UUID uuid);

}
