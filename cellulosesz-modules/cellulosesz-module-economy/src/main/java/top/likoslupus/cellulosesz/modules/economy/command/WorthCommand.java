package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class WorthCommand implements CellCommand {

    private final PlatformService platform;
    private final ItemService items;
    private final WorthService worths;
    private final EconomyService economy;

    public WorthCommand(
            PlatformService platform,
            ItemService items,
            WorthService worths,
            EconomyService economy
    ) {
        this.platform = platform;
        this.items = items;
        this.worths = worths;
        this.economy = economy;
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.worth";
    }

    @Override
    public String usage() {
        return "/worth [hand|inventory|<item> [amount]]";
    }

    @Override
    public String name() {
        return "worth";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 2) {
            invocation.errorKey("common.usage", Map.of("usage", usage()));
            return 0;
        }

        var quantities = new LinkedHashMap<String, Long>();
        if (invocation.args().length == 0 || invocation.args()[0].equalsIgnoreCase("hand")) {
            var player = requirePlayer(invocation);
            if (player.isEmpty()) return 0;
            var snapshot = platform.heldInventorySnapshot(player.orElseThrow());
            if (snapshot.isEmpty()) {
                invocation.errorKey("commands.economy.sell.empty-hand");
                return 0;
            }
            if (!platform.plainInventoryItem(snapshot.orElseThrow())) {
                invocation.errorKey("commands.economy.component-item-unsupported");
                return 0;
            }
            var descriptor = platform.describeInventoryItem(snapshot.orElseThrow());
            if (descriptor.isEmpty()) {
                invocation.errorKey("commands.economy.sell.inventory-changed");
                return 0;
            }
            quantities.put(descriptor.orElseThrow().normalizedItem(), (long) descriptor.orElseThrow().count);
        } else if (invocation.args()[0].equalsIgnoreCase("inventory")) {
            if (invocation.args().length != 1) {
                invocation.errorKey("common.usage", Map.of("usage", usage()));
                return 0;
            }
            var player = requirePlayer(invocation);
            if (player.isEmpty()) return 0;
            var snapshots = platform.inventorySnapshot(player.orElseThrow()).orElseGet(java.util.List::of);
            for (var snapshot : snapshots) {
                if (!platform.plainInventoryItem(snapshot)) {
                    invocation.errorKey("commands.economy.component-item-unsupported");
                    return 0;
                }
                var descriptor = platform.describeInventoryItem(snapshot);
                if (descriptor.isEmpty()) {
                    invocation.errorKey("commands.economy.sell.inventory-changed");
                    return 0;
                }
                quantities.merge(
                        descriptor.orElseThrow().normalizedItem(),
                        (long) descriptor.orElseThrow().count,
                        Math::addExact
                );
            }
        } else {
            var parsed = items.parse(invocation.args()[0]);
            if (parsed.isEmpty()) {
                invocation.errorKey("commands.economy.sell.invalid-item", Map.of("item", invocation.args()[0]));
                return 0;
            }
            var amount = 1L;
            if (invocation.args().length == 2) {
                try {
                    amount = Long.parseLong(invocation.args()[1]);
                    if (amount <= 0L) throw new NumberFormatException();
                } catch (NumberFormatException _) {
                    invocation.errorKey("commands.economy.sell.invalid-amount");
                    return 0;
                }
            }
            quantities.put(parsed.orElseThrow().normalizedItem(), amount);
        }

        if (quantities.isEmpty()) {
            invocation.errorKey("commands.economy.sell.no-sellable-items");
            return 0;
        }

        var rows = new StringBuilder();
        var total = BigDecimal.ZERO;
        var found = 0;
        for (var entry : quantities.entrySet()) {
            var unit = worths.worth(entry.getKey());
            if (unit.isEmpty()) {
                rows.append("\n").append(entry.getKey()).append(" x").append(entry.getValue()).append(" = -");
                continue;
            }
            var lineTotal = unit.orElseThrow().multiply(BigDecimal.valueOf(entry.getValue()));
            total = total.add(lineTotal);
            found++;
            rows.append("\n")
                    .append(entry.getKey())
                    .append(" x")
                    .append(entry.getValue())
                    .append(" = ")
                    .append(economy.format(lineTotal));
        }

        invocation.replyKey(
                "commands.economy.worth-batch",
                Map.of("rows", rows.toString(), "found", found, "total", economy.format(total))
        );
        return found;
    }

    private Optional<CellPlayer> requirePlayer(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) {
            invocation.errorKey("commands.economy.worth-command.error.usage", Map.of("usage", usage()));
        }
        return player;
    }

}
