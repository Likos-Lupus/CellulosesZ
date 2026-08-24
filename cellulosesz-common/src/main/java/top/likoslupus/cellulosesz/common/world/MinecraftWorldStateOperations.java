package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import static java.util.Objects.requireNonNull;

public final class MinecraftWorldStateOperations implements WorldStatePlatformService {

    private final MinecraftServerHandle server;

    public MinecraftWorldStateOperations(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public PlatformResult<Void> setTime(String worldId, long ticks) {
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        var currentServer = server.requireRunning();
        var level = MinecraftWorlds.findLoaded(currentServer, worldId);
        if (level.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WORLD_NOT_FOUND,
                    "World is not loaded"
            );
        }

        var clock = level.orElseThrow().dimensionType().defaultClock();
        if (clock.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.STATE_NOT_ALLOWED,
                    "World does not define a default clock"
            );
        }

        currentServer.clockManager().setTotalTicks(
                clock.orElseThrow(),
                Math.floorMod(ticks, 24_000L)
        );
        currentServer.forceGameTimeSynchronization();
        return PlatformResult.success();
    }

    @Override
    public PlatformResult<Void> setWeather(
            String worldId,
            WeatherType type,
            int durationSeconds
    ) {
        requireNonNull(type, "type");
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        var currentServer = server.requireRunning();
        var level = MinecraftWorlds.findLoaded(currentServer, worldId);
        if (level.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WORLD_NOT_FOUND,
                    "World is not loaded"
            );
        }

        final int ticks;
        try {
            ticks = Math.toIntExact(Math.multiplyExact(durationSeconds, 20L));
        } catch (ArithmeticException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Weather duration is too large"
            );
        }

        switch (type) {
            case CLEAR -> currentServer.setWeatherParameters(
                    ticks,
                    0,
                    false,
                    false
            );
            case RAIN -> currentServer.setWeatherParameters(
                    0,
                    ticks,
                    true,
                    false
            );
            case THUNDER -> currentServer.setWeatherParameters(
                    0,
                    ticks,
                    true,
                    true
            );
        }

        return PlatformResult.success();
    }

}
