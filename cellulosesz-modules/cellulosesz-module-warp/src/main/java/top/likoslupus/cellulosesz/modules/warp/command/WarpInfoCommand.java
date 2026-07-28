package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.util.Map;

public final class WarpInfoCommand extends AbstractWarpCommand {

    public WarpInfoCommand(PlatformService platform, WarpService warps, TeleportService teleports, WarpConfig config) {
        super(platform, warps, teleports, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.warp.info";
    }

    @Override
    public String usage() {
        return "/warpinfo <name>";
    }

    @Override
    public String name() {
        return "warpinfo";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.errorKey("commands.warp.warp-info-command.error.usage", Map.of("usage", usage()));
            return 0;
        }
        try {
            warps.warp(args[0]).whenComplete((warp, failure) -> {
                if (failure != null) invocation.errorKey("service.warp.persistence-failed");
                else if (warp.isEmpty())
                    invocation.errorKey("commands.warp.warp-info-command.error.warp-does-not-exist", Map.of("warp", args[0]));
                else
                    invocation.replyKey("commands.warp.warp-info-command.reply.warp-at", Map.of("warp", warp.orElseThrow().name, "location", warp.orElseThrow().location.compact()));
            });
            return 1;
        } catch (IllegalArgumentException _) {
            invocation.errorKey("commands.warp.warp-info-command.error.warp-does-not-exist", Map.of("warp", args[0]));
            return 0;
        }
    }

}
