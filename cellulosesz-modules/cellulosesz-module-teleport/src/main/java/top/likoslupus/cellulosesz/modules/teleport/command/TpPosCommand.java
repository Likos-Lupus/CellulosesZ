package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

public final class TpPosCommand extends AbstractTeleportCommand {

    public TpPosCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tppos";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/tppos <x> <y> <z> [world]";
    }

    @Override
    public String name() {
        return "tppos";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 3 || args.length > 4) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var self = player(invocation);
        var x = parseDouble(invocation, args[0], "x");
        var y = parseDouble(invocation, args[1], "y");
        var z = parseDouble(invocation, args[2], "z");
        if (self.isEmpty() || x.isEmpty() || y.isEmpty() || z.isEmpty()) return 0;

        var current = platform.location(self.get());
        var world = args.length == 4 ? args[3] : current.world;
        return teleport(
                invocation,
                self.get(),
                new CellLocation(world, x.get(), y.get(), z.get(), current.yaw, current.pitch)
        );
    }

}
