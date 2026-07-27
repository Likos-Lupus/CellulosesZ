package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.ThunderRequest;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Map;

public final class ThunderCommand implements CellCommand {

    private final PlatformService platform;
    private final WorldPlatformService worlds;
    private final WorldConfig config;

    public ThunderCommand(
            PlatformService platform,
            WorldPlatformService worlds,
            WorldConfig config
    ) {
        this.platform = platform;
        this.worlds = worlds;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.thunder";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/thunder <true|false> [seconds]";
    }

    @Override
    public String name() {
        return "thunder";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1 || invocation.args().length > 2) return usage(invocation);
        final boolean enabled;
        if (invocation.args()[0].equalsIgnoreCase("true")) enabled = true;
        else if (invocation.args()[0].equalsIgnoreCase("false")) enabled = false;
        else return usage(invocation);
        final int seconds;
        final int ticks;
        try {
            seconds = invocation.args().length == 2
                    ? Integer.parseInt(invocation.args()[1])
                    : config.defaultWeatherSeconds;
            if (seconds < 1) throw new NumberFormatException();
            ticks = Math.multiplyExact(seconds, 20);
        } catch (NumberFormatException | ArithmeticException failure) {
            invocation.errorKey("commands.world.thunder.invalid-duration");
            return 0;
        }
        var player = platform.player(invocation).orElseThrow();
        var world = platform.location(player).world;
        var result = worlds.setThunder(world, new ThunderRequest(enabled, ticks));
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.world.thunder.success", Map.of(
                "world", world,
                "enabled", enabled,
                "seconds", seconds
        ));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.world.thunder.usage", Map.of("usage", usage()));
        return 0;
    }

}
