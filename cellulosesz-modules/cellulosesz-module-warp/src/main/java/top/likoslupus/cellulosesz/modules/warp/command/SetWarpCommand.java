package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.util.Locale;
import java.util.Map;

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
            invocation.errorKey(
                    "commands.warp.set-warp-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        if (!validName(invocation, args[0])) return 0;

        var existing = warps.warp(args[0]).join();
        if (existing.isPresent()
                && !invocation.hasPermission("cellulosesz.warp.overwrite")
                && !invocation.hasPermission("cellulosesz.warp.overwrite." + args[0].toLowerCase(Locale.ROOT))
        ) {
            invocation.errorKey(
                    "commands.warp.set-warp-command.error.exists",
                    Map.of("warp", args[0])
            );
            return 0;
        }

        try {
            warps.setWarp(args[0], platform.location(self.get()), self.get().uuid()).join();
        } catch (RuntimeException _) {
            invocation.errorKey("service.warp.persistence-failed");
            return 0;
        }

        invocation.replyKey(
                "commands.warp.set-warp-command.reply.1",
                Map.of("value0", args[0])
        );
        return 1;
    }

}
