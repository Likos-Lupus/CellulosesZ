package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldService;

import java.util.Map;

public final class DefaultWorldService implements WorldService {

    private final PlatformService platform;

    public DefaultWorldService(PlatformService platform) {
        this.platform = platform;
    }

    @Override
    public AdminResult setTime(
            String world,
            long time
    ) {
        return platform.setTime(world, time) ? AdminResult.success(
                "service.world.time-set",
                Map.of("world", world, "time", time)
        ) : AdminResult.failure(
                "service.world.time-failed",
                Map.of("world", world)
        );
    }

    @Override
    public AdminResult setWeather(
            String world,
            WeatherType type,
            int seconds
    ) {
        var weather = type.name().toLowerCase();
        return platform.setWeather(world, weather, seconds) ? AdminResult.success(
                "service.world.weather-set",
                Map.of(
                        "world", world,
                        "weather", weather
                )
        ) : AdminResult.failure(
                "service.world.weather-failed",
                Map.of("world", world)
        );
    }

}
