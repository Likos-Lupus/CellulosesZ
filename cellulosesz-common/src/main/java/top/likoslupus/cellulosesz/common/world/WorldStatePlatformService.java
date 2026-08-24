package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.world.WeatherType;

public interface WorldStatePlatformService {

    PlatformResult<Void> setTime(String worldId, long ticks);

    PlatformResult<Void> setWeather(
            String worldId,
            WeatherType type,
            int durationSeconds
    );

}
