package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireFinite;

public interface RandomTeleportCommandService {

    CompletableFuture<TeleportCommandResult> center(
            Optional<CellPlayer> actor, String world,
            Optional<Coordinates> coordinates
    );

    CompletableFuture<TeleportCommandResult> minimum(
            String world,
            Optional<Integer> radius
    );

    CompletableFuture<TeleportCommandResult> maximum(
            String world,
            Optional<Integer> radius
    );

    CompletableFuture<TeleportCommandResult> random(CellPlayer player);

    record Coordinates(
            double x,
            double z
    ) {

        public Coordinates {
            requireFinite(x, "x");
            requireFinite(z, "z");
        }

    }

}
