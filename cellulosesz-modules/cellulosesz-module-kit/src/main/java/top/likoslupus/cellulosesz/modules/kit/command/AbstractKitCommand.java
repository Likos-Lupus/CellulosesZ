package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Optional;

abstract class AbstractKitCommand implements CellCommand {

    protected final PlatformService platform;
    protected final KitService kits;

    AbstractKitCommand(
            PlatformService platform,
            KitService kits
    ) {
        this.platform = platform;
        this.kits = kits;
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) invocation.error("此命令只能由玩家执行。");
        return player;
    }

}
