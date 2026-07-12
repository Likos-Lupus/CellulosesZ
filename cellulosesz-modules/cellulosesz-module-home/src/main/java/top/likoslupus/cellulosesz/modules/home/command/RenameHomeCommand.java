package top.likoslupus.cellulosesz.modules.home.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

import java.util.Map;

public final class RenameHomeCommand extends AbstractHomeCommand {

    public RenameHomeCommand(
            PlatformService platform,
            HomeService homes,
            TeleportService teleports,
            HomeConfig config
    ) {
        super(platform, homes, teleports, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.home.rename";
    }

    @Override
    public String usage() {
        return "/renamehome <old> <new>";
    }

    @Override
    public String name() {
        return "renamehome";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length != 2) {
            invocation.errorKey(
                    "commands.home.rename-home-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        if (!validName(invocation, args[1])) return 0;

        if (homes.renameHome(self.get().uuid(), args[0], args[1]).join()) {
            invocation.replyKey(
                    "commands.home.rename-home-command.reply.1",
                    Map.of(
                            "value0", args[0],
                            "value1", args[1]
                    )
            );
            return 1;
        }

        invocation.errorKey("commands.home.rename-home-command.error.2");
        return 0;
    }

}
