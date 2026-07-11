package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Optional;
import java.util.stream.IntStream;

abstract class AbstractItemCommand implements CellCommand {

    protected final PlatformService platform;
    protected final ItemService items;
    protected final ItemAutomationService automation;
    protected final ItemConfig config;

    AbstractItemCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        this.platform = platform;
        this.items = items;
        this.automation = automation;
        this.config = config;
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) invocation.error("此命令只能由玩家执行。");
        return player;
    }

    protected Optional<CellPlayer> target(CommandInvocation invocation, String name) {
        var player = platform.onlinePlayer(name);
        if (player.isEmpty()) invocation.error("玩家不在线: %s".formatted(name));
        return player;
    }

    protected String join(String[] args, int start) {
        if (start >= args.length) return "";
        var builder = new StringBuilder();
        IntStream.range(start, args.length)
                .forEach(index -> {
                    if (!builder.isEmpty()) builder.append(' ');
                    builder.append(args[index]);
                });
        return builder.toString();
    }

}
