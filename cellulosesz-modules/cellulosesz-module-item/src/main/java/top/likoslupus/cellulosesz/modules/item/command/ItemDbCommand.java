package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.item.ItemStackDetails;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Comparator;
import java.util.Map;

public final class ItemDbCommand implements CellCommand {

    private final PlatformService platform;
    private final InventoryPlatformService inventory;
    private final ItemService items;
    private final ItemConfig config;

    public ItemDbCommand(
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
        return "cellulosesz.command.itemdb";
    }

    @Override
    public String usage() {
        return "/itemdb [item]";
    }

    @Override
    public String name() {
        return "itemdb";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        final ItemStackDetails details;
        if (invocation.args().length == 0) {
            if (!invocation.player()) {
                invocation.errorKey("commands.item.itemdb.console-item-required");
                return 0;
            }
            var result = inventory.heldItemDetails(platform.player(invocation).orElseThrow());
            if (!result.successful() || result.value().isEmpty()) {
                invocation.errorKey("commands.item.itemdb.empty-hand");
                return 0;
            }
            details = result.value().orElseThrow();
        } else {
            var parsed = items.parse(invocation.args()[0]);
            if (parsed.isEmpty() || !items.valid(parsed.orElseThrow())) {
                invocation.errorKey("commands.item.itemdb.unknown", Map.of("item", invocation.args()[0]));
                return 0;
            }
            var descriptor = parsed.orElseThrow();
            details = new ItemStackDetails(
                    descriptor.item,
                    descriptor.item,
                    descriptor.count,
                    Math.max(1, items.maxStackSize(descriptor)),
                    false,
                    false,
                    0,
                    0
            );
        }
        var aliases = config.aliases.entrySet().stream()
                .filter(entry -> normalize(entry.getValue()).equals(normalize(details.itemId())))
                .map(Map.Entry::getKey)
                .sorted(Comparator.naturalOrder())
                .limit(config.maximumDisplayedAliases)
                .toList();
        invocation.replyKey("commands.item.itemdb.result", Map.of(
                "id", details.itemId(),
                "name", details.displayName(),
                "maximum", details.maximumCount(),
                "count", details.count(),
                "components", details.nonDefaultComponents(),
                "aliases", aliases.isEmpty() ? "-" : String.join(", ", aliases),
                "durability", details.damageable()
                        ? details.remainingDurability() + "/" + details.maximumDurability()
                        : "-"
        ));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.itemdb.usage", Map.of("usage", usage()));
        return 0;
    }

    private static String normalize(String value) {
        var result = value.strip().toLowerCase(java.util.Locale.ROOT);
        return result.contains(":") ? result : "minecraft:" + result;
    }

}
