package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

public final class BackCommand extends AbstractTeleportCommand {

    public BackCommand(PlatformService platform, TeleportService teleports) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.back";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "back";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var location = teleports.backLocation(self.get().uuid());
        if (location.isEmpty()) {
            invocation.error("没有可返回的位置。");
            return 0;
        }

        teleports.teleport(self.get(), location.get(), new TeleportOptions().rememberBack(false))
                .thenAccept(result -> {
                    if (result.success()) invocation.reply("已返回上一位置。");
                    else invocation.error("返回失败: " + result.message());
                });

        return 1;
    }

}
