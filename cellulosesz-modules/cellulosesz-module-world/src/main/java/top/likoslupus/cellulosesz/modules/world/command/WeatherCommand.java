package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

public final class WeatherCommand extends AbstractWorldCommand {

    private final WorldService worlds;

    public WeatherCommand(
            PlatformService platform,
            WorldConfig config,
            WorldService worlds
    ) {
        super(platform, config);
        this.worlds = worlds;
    }

    @Override
    public String permission() {
        return "cellulosesz.world.weather";
    }

    @Override
    public String usage() {
        return "/weather <clear|rain|thunder> [seconds] [world]";
    }

    @Override
    public String name() {
        return "weather";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var type = switch (args[0].toLowerCase()) {
            case "clear", "sun" -> WeatherType.CLEAR;
            case "rain", "storm" -> WeatherType.RAIN;
            case "thunder" -> WeatherType.THUNDER;
            default -> null;
        };
        if (type == null) {
            invocation.error("未知天气: " + args[0]);
            return 0;
        }

        var seconds = args.length >= 2
                ? parse(args[1], config.defaultWeatherSeconds)
                : config.defaultWeatherSeconds;
        var result = worlds.setWeather(
                world(invocation, 2),
                type,
                seconds
        );
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

    private int parse(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

}
