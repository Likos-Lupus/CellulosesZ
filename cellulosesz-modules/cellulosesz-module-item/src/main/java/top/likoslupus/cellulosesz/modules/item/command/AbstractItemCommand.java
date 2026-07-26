package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Map;
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
        if (player.isEmpty()) invocation.errorKey("commands.item.abstract-item-command.error.1");
        return player;
    }

    protected Optional<CellPlayer> target(CommandInvocation invocation, String name) {
        var player = invocation.resolvePlayer(name).online();
        if (player.isEmpty())
            invocation.errorKey(
                    "commands.item.abstract-item-command.error.2",
                    Map.of("value0", name)
            );
        return player;
    }

    protected boolean allowSpawn(
            CommandInvocation invocation,
            ItemDescriptor descriptor,
            String permissionRoot
    ) {
        if (!items.valid(descriptor)) {
            invocation.errorKey("commands.item.invalid-item");
            return false;
        }

        if (items.blacklisted(descriptor)
                && !invocation.hasPermission(permissionRoot + ".blacklist")
        ) {
            invocation.errorKey(
                    "commands.item.blacklisted",
                    Map.of("item", descriptor.normalizedItem())
            );
            return false;
        }

        if (descriptor.count > items.maxStackSize(descriptor)
                && !invocation.hasPermission(permissionRoot + ".oversized")
        ) {
            invocation.errorKey(
                    "commands.item.oversized",
                    Map.of(
                            "item", descriptor.normalizedItem(),
                            "maximum", items.maxStackSize(descriptor)
                    )
            );
            return false;
        }

        return true;
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
