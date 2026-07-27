package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;

import java.util.Map;

public final class DepthCommand implements CellCommand {

    private final PlatformService platform;
    private final PlayerStatePlatformService operations;

    public DepthCommand(
            PlatformService platform,
            PlayerStatePlatformService operations
    ) {
        this.platform = platform;
        this.operations = operations;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.depth";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "depth";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 0) {
            invocation.errorKey("commands.playerstate.depth.usage", Map.of("usage", usage()));
            return 0;
        }
        var player = platform.player(invocation).orElseThrow();
        var sea = operations.seaLevel(player);
        if (!sea.successful() || sea.value().isEmpty()) {
            invocation.errorKey("commands.playerstate.depth.platform-failed");
            return 0;
        }
        var y = (int) Math.floor(platform.location(player).y);
        var difference = y - sea.value().orElseThrow();
        var key = difference > 0
                ? "commands.playerstate.depth.above"
                : difference < 0
                        ? "commands.playerstate.depth.below"
                        : "commands.playerstate.depth.equal";
        invocation.replyKey(key, Map.of(
                "distance", Math.abs(difference),
                "y", y,
                "seaLevel", sea.value().orElseThrow()
        ));
        return 1;
    }

}
