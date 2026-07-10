package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;

public final class TpaCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestService requests;
    private final int timeoutSeconds;
    private final boolean here;

    public TpaCommand(
            PlatformService platform,
            TeleportRequestService requests,
            int timeoutSeconds,
            boolean here
    ) {
        this.platform = platform;
        this.requests = requests;
        this.timeoutSeconds = timeoutSeconds;
        this.here = here;
    }

    @Override
    public String permission() {
        return here ? "cellulosesz.teleport.tpahere" : "cellulosesz.teleport.tpa";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/" + name() + " <player>";
    }

    @Override
    public String name() {
        return here ? "tpahere" : "tpa";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var requester = platform.player(invocation);
        var target = platform.onlinePlayer(args[0]);
        if (requester.isEmpty()) {
            invocation.error("此命令只能由玩家执行。");
            return 0;
        }

        if (target.isEmpty()) {
            invocation.error("找不到在线玩家: " + args[0]);
            return 0;
        }

        if (target.get().uuid().equals(requester.get().uuid())) {
            invocation.error("不能向自己发送传送请求。");
            return 0;
        }

        requests.create(
                requester.get(),
                target.get(),
                here ? TeleportRequestType.TARGET_TO_REQUESTER : TeleportRequestType.REQUESTER_TO_TARGET,
                timeoutSeconds
        );

        invocation.reply("已向 " + target.get().name() + " 发送传送请求。请求将在 " + timeoutSeconds + " 秒后过期。");
        return 1;
    }

}
