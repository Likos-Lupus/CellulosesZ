package top.likoslupus.cellulosesz.modules.home.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

public final class DelHomeCommand extends AbstractHomeCommand {

    public DelHomeCommand(
            PlatformService platform,
            HomeService homes,
            TeleportService teleports,
            HomeConfig config
    ) {
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
            invocation.error("用法: " + usage());
            return 0;
        }
        if (homes.deleteHome(self.get().uuid(), args[0]).join()) {
            invocation.reply("已删除 Home: " + args[0]);
            return 1;
        }
        invocation.error("Home 不存在: " + args[0]);
        return 0;
    }

}
