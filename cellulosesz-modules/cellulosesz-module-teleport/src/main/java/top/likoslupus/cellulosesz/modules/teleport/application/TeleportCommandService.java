package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TeleportCommandService {

    CompletableFuture<TeleportCommandResult> tp(
            Optional<CellPlayer> actor,
            String first,
            Optional<String> second,
            boolean override,
            boolean bypassPreference
    );

    CompletableFuture<TeleportCommandResult> here(
            CellPlayer actor,
            String target,
            boolean override,
            boolean bypassPreference
    );

    CompletableFuture<TeleportCommandResult> position(
            CellPlayer actor,
            double x, double y, double z,
            Optional<String> world
    );

    CompletableFuture<TeleportCommandResult> all(
            Optional<CellPlayer> actor,
            Optional<String> destination,
            boolean bypassPreference
    );

    CompletableFuture<TeleportCommandResult> offline(CellPlayer actor, String target);

    CompletableFuture<TeleportCommandResult> back(CellPlayer actor);

    CompletableFuture<TeleportCommandResult> jump(CellPlayer actor, int maximumDistance);

    CompletableFuture<TeleportCommandResult> top(CellPlayer actor);

    CompletableFuture<TeleportCommandResult> bottom(CellPlayer actor);

    CompletableFuture<TeleportCommandResult> world(CellPlayer actor, String world);

}
