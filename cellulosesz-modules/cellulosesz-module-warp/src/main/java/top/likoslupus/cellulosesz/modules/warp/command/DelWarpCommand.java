package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.util.Map;

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
            invocation.errorKey(
                    "commands.warp.del-warp-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        boolean deleted;
        try {
            deleted = warps.deleteWarp(args[0]).join();
        } catch (RuntimeException _) {
            invocation.errorKey("service.warp.persistence-failed");
            return 0;
        }

        if (deleted) {
            invocation.replyKey(
                    "commands.warp.del-warp-command.reply.1",
                    Map.of("value0", args[0])
            );
            return 1;
        }

        invocation.errorKey(
                "commands.warp.del-warp-command.error.2",
                Map.of("value0", args[0])
        );
        return 0;
    }

}
