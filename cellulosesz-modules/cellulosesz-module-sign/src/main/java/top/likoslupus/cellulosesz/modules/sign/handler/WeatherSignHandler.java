package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldService;

import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class WeatherSignHandler implements SynchronousSignHandler {

    private final WorldDirectory worldDirectory;
    private final WorldService worlds;

    public WeatherSignHandler(
            WorldDirectory worldDirectory,
            WorldService worlds
    ) {
        this.worldDirectory = requireNonNull(worldDirectory, "worldDirectory");
        this.worlds = requireNonNull(worlds, "worlds");
    }

    @Override
    public String id() {
        return "Weather";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        if (type(context).isEmpty()) {
            return SignUseResult.failure("service.sign.weather-format");
        }

        if (!context.line(2).isBlank()
                && SignHandlerSupport.count(context.line(2), 1, 86_400).isEmpty()
        ) {
            return SignUseResult.failure("service.sign.weather-format");
        }

        if (!context.line(3).isBlank()
                && worldDirectory.resolveLoadedWorld(context.line(3)).isEmpty()
        ) {
            return SignUseResult.failure("service.sign.weather-world");
        }

        return SignUseResult.success("service.sign.valid");
    }

    private Optional<WeatherType> type(SignUseContext context) {
        try {
            return Optional.of(WeatherType.valueOf(context.line(1).toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var type = type(context);
        if (type.isEmpty()) {
            return SignUseResult.failure("service.sign.weather-format");
        }

        var seconds = SignHandlerSupport
                .count(context.line(2), 1, 86_400)
                .orElse(300);
        var requestedWorld = context.line(3).isBlank()
                ? context.location().world
                : context.line(3);
        var world = worldDirectory.resolveLoadedWorld(requestedWorld);

        if (world.isEmpty()) {
            return SignUseResult.failure("service.sign.weather-world");
        }

        return SignHandlerSupport.admin(worlds.setWeather(
                world.orElseThrow(),
                type.orElseThrow(),
                seconds
        ));
    }

}
