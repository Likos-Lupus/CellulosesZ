package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

public final class TpCommand extends AbstractTeleportCommand {

    public TpCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tp";
    }

    @Override
    public String usage() {
        return "/tp <target> 或 /tp <player> <target>";
    }

    @Override
    public String name() {
        return "tp";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length == 1) {
            var self = player(invocation);
            var target = online(invocation, args[0]);
            if (self.isEmpty() || target.isEmpty()) return 0;
            return teleport(invocation, self.get(), platform.location(target.get()));
        }

        if (args.length == 2) {
            var subject = online(invocation, args[0]);
            var target = online(invocation, args[1]);
            if (subject.isEmpty() || target.isEmpty()) return 0;
            return teleport(invocation, subject.get(), platform.location(target.get()));
        }

        invocation.error("用法: " + usage());
        return 0;
    }

}
