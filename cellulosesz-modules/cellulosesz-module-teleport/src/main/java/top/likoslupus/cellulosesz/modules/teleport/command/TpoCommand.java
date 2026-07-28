package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.Map;

public final class TpoCommand extends AbstractTeleportCommand {

    public TpoCommand(PlatformService platform, TeleportService teleports) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpo";
    }

    @Override
    public String usage() {
        return "/tpo <target> | /tpo <player> <target>";
    }

    @Override
    public String name() {
        return "tpo";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length == 1) {
            var self = player(invocation);
            var target = online(invocation, args[0]);
            if (self.isEmpty() || target.isEmpty()) return 0;
            return teleport(invocation, self.orElseThrow(), platform.location(target.orElseThrow()));
        }
        if (args.length == 2) {
            var subject = online(invocation, args[0]);
            var target = online(invocation, args[1]);
            if (subject.isEmpty() || target.isEmpty()) return 0;
            return teleport(invocation, subject.orElseThrow(), platform.location(target.orElseThrow()));
        }
        invocation.errorKey("commands.teleport.tp-command.error.usage", Map.of("usage", usage()));
        return 0;
    }

}
