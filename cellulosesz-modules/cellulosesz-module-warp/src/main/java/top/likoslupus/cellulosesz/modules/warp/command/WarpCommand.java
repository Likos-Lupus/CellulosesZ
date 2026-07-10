package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.util.List;

public final class WarpCommand extends AbstractWarpCommand {

    public WarpCommand(
            PlatformService platform,
            WarpService warps,
            TeleportService teleports,
            WarpConfig config
    ) {
        super(platform, warps, teleports, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("warps");
    }

    @Override
    public String permission() {
        return "cellulosesz.warp.use";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/warp <name> 或 /warps";
    }

    @Override
    public String name() {
        return "warp";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        if (invocation.label().equalsIgnoreCase("warps")) {
            var names = warps.warps().join().stream()
                    .map(warp -> warp.name)
                    .toList();
            invocation.reply(names.isEmpty() ? "当前没有 Warp。" : "Warp: " + String.join(", ", names));
            return 1;
        }

        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var warp = warps.warp(args[0]).join();
        if (warp.isEmpty()) {
            invocation.error("Warp 不存在: " + args[0]);
            return 0;
        }

        if (warp.get().permission != null && !warp.get().permission.isBlank() && !invocation.hasPermission(warp.get().permission)) {
            invocation.error("你没有权限使用此 Warp。");
            return 0;
        }

        teleports.teleport(self.get(), warp.get().location, options(invocation))
                .thenAccept(result -> {
                    if (result.success()) invocation.reply("已传送到 Warp: " + warp.get().displayName);
                    else invocation.error("传送失败: " + result.message());
                });
        return 1;
    }

}
