package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldResult;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.common.world.WorldStatePlatformService;

import static java.util.Objects.requireNonNull;

public final class DefaultWorldService implements WorldService {

    private final WorldStatePlatformService platform;

    public DefaultWorldService(WorldStatePlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    @Override
    public WorldResult setTime(String world, long time) {
        var result = platform.setTime(world, time);
        return result.successful()
                ?
                WorldResult.success(
                        "service.world.time-set",
                        MessageArguments.builder().add(world).add(time).build()
                )
                : WorldResult.failure(
                        "service.world.time-failed",
                        MessageArguments.builder().add(world).build()
                );
    }

    @Override
    public WorldResult setWeather(String world, WeatherType type, int seconds) {
        var result = platform.setWeather(world, type, seconds);
        return result.successful()
                ?
                WorldResult.success(
                        "service.world.weather-set",
                        MessageArguments.builder()
                                .add(world)
                                .add(type.name().toLowerCase())
                                .build()
                )
                : WorldResult.failure(
                        "service.world.weather-failed",
                        MessageArguments.builder().add(world).build()
                );
    }

}
