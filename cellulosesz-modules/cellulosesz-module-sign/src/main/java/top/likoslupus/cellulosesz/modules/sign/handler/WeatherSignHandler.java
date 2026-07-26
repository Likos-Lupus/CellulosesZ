package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldService;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class WeatherSignHandler implements SynchronousSignHandler {

    private static final Set<String> TYPES = Set.of("clear", "rain", "thunder");
    private final PlatformService platform;
    private final WorldService worlds;

    public WeatherSignHandler(PlatformService platform, WorldService worlds) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
    }

    @Override
    public String id() {
        return "Weather";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        if (!TYPES.contains(context.line(1).toLowerCase(Locale.ROOT))) {
            return SignUseResult.failure("service.sign.weather-format");
        }
        if (!context.line(2).isBlank()
                && SignHandlerSupport.count(context.line(2), 1, 86400).isEmpty()) {
            return SignUseResult.failure("service.sign.weather-format");
        }
        if (!context.line(3).isBlank() && !platform.worlds().contains(context.line(3))) {
            return SignUseResult.failure("service.sign.weather-world");
        }
        return SignUseResult.success("service.sign.valid");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var value = context.line(1).toUpperCase(Locale.ROOT);
        if (!TYPES.contains(context.line(1).toLowerCase(Locale.ROOT))) {
            return SignUseResult.failure("service.sign.weather-format");
        }
        var seconds = SignHandlerSupport.count(context.line(2), 1, 86400).orElse(300);
        var world = context.line(3).isBlank() ? context.location().world : context.line(3);
        return SignHandlerSupport.admin(worlds.setWeather(world, WeatherType.valueOf(value), seconds));
    }

}
