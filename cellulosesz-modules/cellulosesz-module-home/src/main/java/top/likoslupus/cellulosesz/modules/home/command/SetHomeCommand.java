package top.likoslupus.cellulosesz.modules.home.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

public final class SetHomeCommand extends AbstractHomeCommand {

    public SetHomeCommand(
            PlatformService platform,
            HomeService homes,
            TeleportService teleports,
            HomeConfig config
    ) {
        super(platform, homes, teleports, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.home.set";
    }

    @Override
    public String usage() {
        return "/sethome [name]";
    }

    @Override
    public String name() {
        return "sethome";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var name = nameOrDefault(invocation.args());
        if (!validName(invocation, name)) return 0;

        var existing = homes.homes(self.get().uuid()).join();
        if (!existing.containsKey(name.toLowerCase())
                && existing.size() >= config.limits.defaultMaxHomes
                && !invocation.hasPermission("cellulosesz.home.bypass-limit")
        ) {
            invocation.error("Home 数量已达到上限: " + config.limits.defaultMaxHomes);
            return 0;
        }

        homes.setHome(self.get().uuid(), name, platform.location(self.get())).join();
        invocation.reply("已设置 Home: " + name);

        return 1;
    }

}
