package top.likoslupus.cellulosesz.modules.admin.application;

import top.likoslupus.cellulosesz.api.admin.AdminActor;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PlayerControlCommandService {

    CompletableFuture<AdminResult> burn(String player, int seconds);

    CompletableFuture<AdminResult> extinguish(Optional<CellPlayer> actor, Optional<String> target);

    CompletableFuture<AdminResult> ice(Optional<CellPlayer> actor, Optional<String> target);

    CompletableFuture<AdminResult> kill(String player, boolean force);

    CompletableFuture<AdminResult> suicide(CellPlayer player);

    CompletableFuture<AdminResult> sudo(
            AdminActor actor,
            String player,
            String command
    );

}
