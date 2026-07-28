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

    public SetWarpCommand(PlatformService platform, WarpService warps, TeleportService teleports, WarpConfig config) {
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
            invocation.errorKey("commands.warp.set-warp-command.error.usage", Map.of("usage", usage()));
            return 0;
        }
        if (!validName(invocation, args[0])) return 0;
        var key = args[0].toLowerCase(Locale.ROOT);
        warps.warp(key).whenComplete((existing, loadFailure) -> {
            if (loadFailure != null) {
                invocation.errorKey("service.warp.persistence-failed");
                return;
            }
            if (existing.isPresent()
                    && !invocation.hasPermission("cellulosesz.warp.overwrite")
                    && !invocation.hasPermission("cellulosesz.warp.overwrite." + key)) {
                invocation.errorKey("commands.warp.set-warp-command.error.exists", Map.of("warp", args[0]));
                return;
            }
            platform.callOnServerThread(() -> platform.location(self.orElseThrow()))
                    .thenCompose(location -> warps.setWarp(key, location, self.orElseThrow().uuid()))
                    .whenComplete((_, saveFailure) -> {
                        if (saveFailure != null) invocation.errorKey("service.warp.persistence-failed");
                        else
                            invocation.replyKey("commands.warp.set-warp-command.reply.set-warp", Map.of("warp", args[0]));
                    });
        });
        return 1;
    }

}
