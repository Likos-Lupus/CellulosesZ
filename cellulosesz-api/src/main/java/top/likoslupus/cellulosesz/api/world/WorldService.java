package top.likoslupus.cellulosesz.api.world;

import top.likoslupus.cellulosesz.api.admin.AdminResult;

public interface WorldService {

    AdminResult setTime(String world, long time);

    AdminResult setWeather(
            String world,
            WeatherType type,
            int seconds
    );

}
