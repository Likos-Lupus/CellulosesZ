package top.likoslupus.cellulosesz.modules.home.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

import java.util.List;

public final class HomeCommand extends AbstractHomeCommand {

    public HomeCommand(
            PlatformService platform,
            HomeService homes,
            TeleportService teleports,
            HomeConfig config
    ) {
        super(platform, homes, teleports, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("homes");
    }

    @Override
    public String permission() {
        return "cellulosesz.home.use";
    }

    @Override
    public String usage() {
        return "/home [name] 或 /homes";
    }

    @Override
    public String name() {
        return "home";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (invocation.label().equalsIgnoreCase("homes")) {
            var names = homes.homes(self.get().uuid()).join().keySet();
            invocation.reply(names.isEmpty() ? "你还没有设置 Home。" : "Home: " + String.join(", ", names));
            return 1;
        }

        var name = nameOrDefault(args);
        var location = homes.home(self.get().uuid(), name).join();
        if (location.isEmpty()) {
            invocation.error("Home 不存在: " + name);
            return 0;
        }

        teleports.teleport(self.get(), location.get(), options(invocation))
                .thenAccept(result -> {
                    if (result.success()) invocation.reply("已传送到 Home: " + name);
                    else invocation.error("传送失败: " + result.message());
                });

        return 1;
    }

}
