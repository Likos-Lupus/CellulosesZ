package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

public final class TopCommand extends AbstractTeleportCommand {

    public TopCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.top";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "top";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var current = platform.location(self.get());
        var top = platform.highestLocation(current.world, current.x, current.z);
        if (top.isEmpty()) {
            invocation.error("找不到顶部位置。");
            return 0;
        }

        return teleport(invocation, self.get(), top.get());
    }

}
