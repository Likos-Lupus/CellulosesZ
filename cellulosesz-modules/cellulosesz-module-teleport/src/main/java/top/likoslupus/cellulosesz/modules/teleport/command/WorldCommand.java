package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

public final class WorldCommand extends AbstractTeleportCommand {

    public WorldCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.world";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/world <world>";
    }

    @Override
    public String name() {
        return "world";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length == 0) {
            invocation.reply("可用世界: " + String.join(", ", platform.worlds()));
            return 1;
        }

        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var current = platform.location(self.get());
        var target = current.withWorld(args[0]);
        return teleport(invocation, self.get(), target);
    }

}
