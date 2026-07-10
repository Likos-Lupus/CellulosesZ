package top.likoslupus.cellulosesz.api.platform;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PlatformService {

    Optional<CellPlayer> player(CommandInvocation invocation);

    Optional<CellPlayer> player(Object nativeHandle);

    Optional<CellPlayer> onlinePlayer(String name);

    List<CellPlayer> onlinePlayers();

    List<String> worlds();

    String defaultWorld();

    CellLocation location(CellPlayer player);

    CompletableFuture<Boolean> teleport(CellPlayer player, CellLocation location);

    Optional<CellLocation> safeLocation(CellLocation location);

    Optional<CellLocation> highestLocation(
            String world,
            double x,
            double z
    );

    Optional<CellLocation> targetLocation(CellPlayer player, int maxDistance);

    default void sendMessage(CellPlayer player, String message) {
    }

    default boolean dispatchConsoleCommand(String command) {
        return false;
    }

}
