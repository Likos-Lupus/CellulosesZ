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

    default void kick(CellPlayer player, String reason) {
    }

    default boolean setFlying(CellPlayer player, boolean enabled) {
        return false;
    }

    default boolean setInvulnerable(CellPlayer player, boolean enabled) {
        return false;
    }

    default boolean heal(CellPlayer player) {
        return false;
    }

    default boolean feed(CellPlayer player) {
        return false;
    }

    default boolean setTime(String world, long time) {
        return false;
    }

    default boolean setWeather(
            String world,
            String weather,
            int seconds
    ) {
        return false;
    }

    default int removeEntities(
            String selector,
            CellPlayer origin,
            int radius
    ) {
        return -1;
    }

    default boolean dispatchConsoleCommand(String command) {
        return false;
    }

}
