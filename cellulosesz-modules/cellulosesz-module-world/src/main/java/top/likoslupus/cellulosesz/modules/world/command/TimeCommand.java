package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Map;

public final class TimeCommand extends AbstractWorldCommand {

    private final WorldService worlds;

    public TimeCommand(
            PlatformService platform,
            WorldConfig config,
            WorldService worlds
    ) {
        super(platform, config);
        this.worlds = worlds;
    }

    @Override
    public String permission() {
        return "cellulosesz.world.time";
    }

    @Override
    public String usage() {
        return "/time <day|noon|night|midnight|ticks> [world]";
    }

    @Override
    public String name() {
        return "time";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1) {
            invocation.errorKey(
                    "commands.world.time-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var time = switch (args[0].toLowerCase()) {
            case "day" -> 1000L;
            case "noon" -> 6000L;
            case "night" -> 13000L;
            case "midnight" -> 18000L;
            default -> parse(args[0]);
        };
        if (time < 0L) {
            invocation.errorKey(
                    "commands.world.time-command.error.2",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        var result = worlds.setTime(world(invocation, 1), time);
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

    private long parse(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException _) {
            return -1L;
        }
    }

}
