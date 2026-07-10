package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

public final class TprCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportService teleports;
    private final RandomTeleportService randomTeleports;
    private final int minRadius;
    private final int maxRadius;

    public TprCommand(
            PlatformService platform,
            TeleportService teleports,
            RandomTeleportService randomTeleports,
            int minRadius,
            int maxRadius
    ) {
        this.platform = platform;
        this.teleports = teleports;
        this.randomTeleports = randomTeleports;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.random";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "tpr";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.error("此命令只能由玩家执行。");
            return 0;
        }

        var current = platform.location(self.get());
        var location = randomTeleports.randomLocation(current.world, minRadius, maxRadius);
        if (location.isEmpty()) {
            invocation.error("无法找到随机传送位置。");
            return 0;
        }

        teleports.teleport(self.get(), location.get(), new TeleportOptions());
        invocation.reply("正在随机传送...");
        return 1;
    }

}
