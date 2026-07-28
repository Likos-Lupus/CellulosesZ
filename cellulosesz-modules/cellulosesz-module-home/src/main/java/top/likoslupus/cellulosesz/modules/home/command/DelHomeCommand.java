package top.likoslupus.cellulosesz.modules.home.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

import java.util.Map;

public final class DelHomeCommand extends AbstractHomeCommand {

    public DelHomeCommand(PlatformService platform, HomeService homes, TeleportService teleports, HomeConfig config) {
        super(platform, homes, teleports, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.home.delete";
    }

    @Override
    public String usage() {
        return "/delhome <name>";
    }

    @Override
    public String name() {
        return "delhome";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;
        var args = invocation.args();
        if (args.length != 1) {
            invocation.errorKey("commands.home.del-home-command.error.usage", Map.of("usage", usage()));
            return 0;
        }
        homes.deleteHome(self.orElseThrow().uuid(), args[0]).whenComplete((deleted, failure) -> {
            if (failure != null) invocation.errorKey("common.persistence-failed");
            else if (deleted)
                invocation.replyKey("commands.home.del-home-command.reply.deleted-home", Map.of("home", args[0]));
            else
                invocation.errorKey("commands.home.del-home-command.error.home-does-not-exist", Map.of("home", args[0]));
        });
        return 1;
    }

}
