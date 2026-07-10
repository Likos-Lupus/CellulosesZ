package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;

public final class TpDenyCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestService requests;

    public TpDenyCommand(
            PlatformService platform,
            TeleportRequestService requests
    ) {
        this.platform = platform;
        this.requests = requests;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpdeny";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "tpdeny";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.error("此命令只能由玩家执行。");
            return 0;
        }

        if (requests.removeFor(self.get().uuid()).isPresent()) {
            invocation.reply("已拒绝传送请求。");
            return 1;
        }

        invocation.error("没有待处理的传送请求。");
        return 0;
    }

}
