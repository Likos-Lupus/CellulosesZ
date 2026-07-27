package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Map;

public final class MoreCommand implements CellCommand {

    private final PlatformService platform;
    private final InventoryPlatformService inventory;
    private final ItemService items;
    private final ItemConfig config;

    public MoreCommand(
            PlatformService platform,
            InventoryPlatformService inventory,
            ItemService items,
            ItemConfig config
    ) {
        this.platform = platform;
        this.inventory = inventory;
        this.items = items;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.more";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/more [amount]";
    }

    @Override
    public String name() {
        return "more";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        var player = platform.player(invocation).orElseThrow();
        var details = inventory.heldItemDetails(player);
        if (!details.successful() || details.value().isEmpty()) {
            invocation.errorKey("commands.item.more.empty-hand");
            return 0;
        }
        var held = details.value().orElseThrow();
        var descriptor = items.parse(held.itemId());
        if (descriptor.isPresent() && items.blacklisted(descriptor.orElseThrow())) {
            invocation.errorKey("commands.item.more.blacklisted", Map.of("item", held.itemId()));
            return 0;
        }
        final int target;
        try {
            if (invocation.args().length == 0) target = held.maximumCount();
            else {
                var increase = Integer.parseInt(invocation.args()[0]);
                if (increase <= 0) return usage(invocation);
                target = Math.addExact(held.count(), increase);
            }
        } catch (NumberFormatException | ArithmeticException failure) {
            invocation.errorKey("commands.item.more.invalid-amount");
            return 0;
        }
        var oversized = target > held.maximumCount();
        if (oversized && (!config.allowOversizedStacks || !invocation.hasPermission("cellulosesz.command.more.oversized"))) {
            invocation.errorKey("commands.item.more.maximum", Map.of("maximum", held.maximumCount()));
            return 0;
        }
        var permitted = oversized ? config.maximumOversizedStack : held.maximumCount();
        if (target > permitted) {
            invocation.errorKey("commands.item.more.maximum", Map.of("maximum", permitted));
            return 0;
        }
        var result = inventory.setHeldCount(player, target, permitted);
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.item.more.success", Map.of(
                "item", result.value().orElseThrow().itemId(),
                "previous", result.value().orElseThrow().previousCount(),
                "current", result.value().orElseThrow().currentCount()
        ));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.more.usage", Map.of("usage", usage()));
        return 0;
    }

}
