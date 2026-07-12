package top.likoslupus.cellulosesz.api.player;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerResolver {

    ResolvedPlayer resolveKnown(String input, @Nullable CellPlayer viewer);

    ResolvedPlayer resolveKnown(UUID uuid, @Nullable CellPlayer viewer);

    CompletableFuture<ResolvedPlayer> resolve(String input, @Nullable CellPlayer viewer);

}
