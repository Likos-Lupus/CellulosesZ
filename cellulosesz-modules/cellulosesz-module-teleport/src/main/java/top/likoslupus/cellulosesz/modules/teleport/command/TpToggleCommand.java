package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

public final class TpToggleCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;

    public TpToggleCommand(
            PlatformService platform,
            UserService users
    ) {
        this.platform = platform;
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tptoggle";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "tptoggle";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.error("此命令只能由玩家执行。");
            return 0;
        }

        var user = users.load(self.get().uuid()).join();
        user.preferences.teleportRequests = !user.preferences.teleportRequests;
        users.markDirty(self.get().uuid());
        invocation.reply("传送请求接收状态: " + (user.preferences.teleportRequests ? "开启" : "关闭"));
        return 1;
    }

}
