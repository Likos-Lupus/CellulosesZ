package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

public final class SetWarpCommand extends AbstractWarpCommand {

    public SetWarpCommand(
            PlatformService platform,
            WarpService warps,
            TeleportService teleports,
            WarpConfig config
    ) {
        super(platform, warps, teleports, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.warp.create";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/setwarp <name>";
    }

    @Override
    public String name() {
        return "setwarp";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: " + usage());
            return 0;
        }
        if (!validName(invocation, args[0])) return 0;

        warps.setWarp(args[0], platform.location(self.get()), self.get().uuid()).join();
        invocation.reply("已设置 Warp: " + args[0]);
        return 1;
    }

}
