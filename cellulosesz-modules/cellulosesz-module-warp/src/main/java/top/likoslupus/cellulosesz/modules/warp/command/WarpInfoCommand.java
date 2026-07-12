package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.util.Map;

public final class WarpInfoCommand extends AbstractWarpCommand {

    public WarpInfoCommand(
            PlatformService platform,
            WarpService warps,
            TeleportService teleports,
            WarpConfig config
    ) {
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
            invocation.errorKey(
                    "commands.warp.warp-info-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var warp = warps.warp(args[0]).join();
        if (warp.isEmpty()) {
            invocation.errorKey(
                    "commands.warp.warp-info-command.error.2",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        invocation.replyKey(
                "commands.warp.warp-info-command.reply.1",
                Map.of(
                        "value0", warp.get().name,
                        "value1", warp.get().location.compact()
                )
        );
        return 1;
    }

}
