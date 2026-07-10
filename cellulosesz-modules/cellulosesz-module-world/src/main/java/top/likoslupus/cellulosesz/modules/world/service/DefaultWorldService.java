package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldService;

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
        return platform.setTime(world, time)
                ? AdminResult.success("已设置世界 %s 的时间为 %d。".formatted(world, time))
                : AdminResult.failure("设置时间失败: %s".formatted(world));
    }

    @Override
    public AdminResult setWeather(
            String world,
            WeatherType type,
            int seconds
    ) {
        return platform.setWeather(world, type.name().toLowerCase(), seconds)
                ? AdminResult.success("已设置世界 %s 的天气为 %s。".formatted(world, type.name().toLowerCase()))
                : AdminResult.failure("设置天气失败: %s".formatted(world));
    }

}
