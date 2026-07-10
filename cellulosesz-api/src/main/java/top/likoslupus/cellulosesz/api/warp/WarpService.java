package top.likoslupus.cellulosesz.api.warp;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface WarpService {

    CompletableFuture<List<Warp>> warps();

    CompletableFuture<Optional<Warp>> warp(String name);

    CompletableFuture<Warp> setWarp(
            String name,
            CellLocation location,
            UUID creator
    );

    CompletableFuture<Boolean> deleteWarp(String name);

    CompletableFuture<Void> reload();

}
