package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TeleportPreferenceCommandService {

    CompletableFuture<TeleportCommandResult> autoAccept(
            CellPlayer player,
            Optional<Boolean> requested
    );

    CompletableFuture<TeleportCommandResult> toggle(
            CellPlayer actor,
            Optional<String> target,
            Optional<Boolean> requested
    );

}
