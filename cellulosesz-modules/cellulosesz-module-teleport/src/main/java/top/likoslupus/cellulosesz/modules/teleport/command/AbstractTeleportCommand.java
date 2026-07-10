package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.Optional;

abstract class AbstractTeleportCommand implements CellCommand {

    protected final PlatformService platform;
    protected final TeleportService teleports;

    AbstractTeleportCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        this.platform = platform;
        this.teleports = teleports;
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) {
            invocation.error("此命令只能由玩家执行。");
        }
        return player;
    }

    protected Optional<CellPlayer> online(CommandInvocation invocation, String name) {
        var player = platform.onlinePlayer(name);
        if (player.isEmpty()) {
            invocation.error("找不到在线玩家: " + name);
        }
        return player;
    }

    protected int teleport(
            CommandInvocation invocation,
            CellPlayer player,
            CellLocation location
    ) {
        teleports.teleport(player, location, new TeleportOptions())
                .thenAccept(result -> {
                    if (result.success()) {
                        invocation.reply("已传送到 " + result.location().compact());
                    } else {
                        invocation.error("传送失败: " + result.message());
                    }
                });
        return 1;
    }

    protected Optional<Double> parseDouble(
            CommandInvocation invocation,
            String value,
            String name
    ) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            invocation.error(name + " 必须是数字: " + value);
            return Optional.empty();
        }
    }

    protected Optional<Integer> parseInt(
            CommandInvocation invocation,
            String value,
            String name
    ) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            invocation.error(name + " 必须是整数: " + value);
            return Optional.empty();
        }
    }

}
