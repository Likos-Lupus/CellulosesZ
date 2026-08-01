package top.likoslupus.cellulosesz.api.world;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface WorldStatePlatformService {

    PlatformResult<Void> setTime(String worldId, long ticks);

    PlatformResult<Void> setWeather(
            String worldId,
            WeatherType type,
            int durationSeconds
    );

}
