package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.api.world.WorldStatePlatformService;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class DefaultWorldService implements WorldService {

    private final WorldStatePlatformService platform;

    public DefaultWorldService(WorldStatePlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    @Override
    public AdminResult setTime(String world, long time) {
        var result = platform.setTime(world, time);
        return result.successful()
                ?
                AdminResult.success(
                        "service.world.time-set",
                        Map.of("world", world, "time", time)
                )
                : AdminResult.failure(
                        "service.world.time-failed",
                        Map.of("world", world)
                );
    }

    @Override
    public AdminResult setWeather(String world, WeatherType type, int seconds) {
        var result = platform.setWeather(world, type, seconds);
        return result.successful()
                ?
                AdminResult.success(
                        "service.world.weather-set",
                        Map.of("world", world, "weather", type.name().toLowerCase())
                )
                : AdminResult.failure(
                        "service.world.weather-failed",
                        Map.of("world", world)
                );
    }

}
