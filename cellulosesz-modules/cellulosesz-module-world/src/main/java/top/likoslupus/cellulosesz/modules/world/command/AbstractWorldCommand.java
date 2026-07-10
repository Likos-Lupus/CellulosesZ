package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Optional;

abstract class AbstractWorldCommand implements CellCommand {

    protected final PlatformService platform;
    protected final WorldConfig config;

    AbstractWorldCommand(
            PlatformService platform,
            WorldConfig config
    ) {
        this.platform = platform;
        this.config = config;
    }

    protected String world(
            CommandInvocation invocation,
            int index
    ) {
        if (invocation.args().length > index) return invocation.args()[index];
        var player = platform.player(invocation);
        return player
                .map(value -> platform.location(value).world)
                .orElseGet(platform::defaultWorld);
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        return platform.player(invocation);
    }

}
