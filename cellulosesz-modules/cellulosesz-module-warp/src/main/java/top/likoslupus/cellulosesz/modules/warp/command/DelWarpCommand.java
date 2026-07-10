package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

public final class DelWarpCommand extends AbstractWarpCommand {

    public DelWarpCommand(
            PlatformService platform,
            WarpService warps,
            TeleportService teleports,
            WarpConfig config
    ) {
        super(platform, warps, teleports, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.warp.delete";
    }

    @Override
    public String usage() {
        return "/delwarp <name>";
    }

    @Override
    public String name() {
        return "delwarp";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        if (warps.deleteWarp(args[0]).join()) {
            invocation.reply("已删除 Warp: " + args[0]);
            return 1;
        }

        invocation.error("Warp 不存在: " + args[0]);
        return 0;
    }

}
