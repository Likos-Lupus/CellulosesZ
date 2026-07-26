package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.item.InventoryStackSelection;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.math.BigDecimal;
import java.util.*;

public final class SellCommand implements CellCommand {

    private final PlatformService platform;
    private final ItemService items;
    private final WorthService worths;
    private final EconomyService economy;
    private final CellulosesZLogger logger;

    public SellCommand(
            PlatformService platform,
            ItemService items,
            WorthService worths,
            EconomyService economy,
            CellulosesZLogger logger
    ) {
        this.platform = platform;
        this.items = items;
        this.worths = worths;
        this.economy = economy;
        this.logger = logger;
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.sell";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/sell <hand|all|item> [amount]";
    }

    @Override
    public String name() {
        return "sell";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1 || invocation.args().length > 2) {
            invocation.errorKey("common.usage", Map.of("usage", usage()));
            return 0;
        }
        var player = platform.player(invocation);
        if (player.isEmpty()) {
            invocation.errorKey("commands.economy.sell.player-only");
            return 0;
        }

        var requested = parseRequested(invocation);
        if (requested.isEmpty()) return 0;
        var sale = select(invocation, player.orElseThrow(), requested.orElseThrow());
        if (sale.isEmpty()) return 0;

        var total = sale.stream()
                .map(line -> line.unitWorth().multiply(BigDecimal.valueOf(line.selection().count())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            invocation.errorKey("commands.economy.sell.no-worth");
            return 0;
        }

        var mutation = platform.prepareInventoryRemoval(
                player.orElseThrow(), sale.stream().map(SaleLine::selection).toList()
        );
        if (mutation.isEmpty() || !mutation.orElseThrow().commit()) {
            invocation.errorKey("commands.economy.sell.inventory-changed");
            return 0;
        }

        var removedCount = sale.stream().mapToInt(line -> line.selection().count()).sum();
        economy.deposit(
                player.orElseThrow().uuid(),
                total,
                TransactionCause.command(player.orElseThrow().name(), "sell")
        ).whenComplete((result, failure) -> platform.runOnServerThread(() -> {
            if (failure != null || !result.success()) {
                var rolledBack = mutation.orElseThrow().rollback();
                if (!rolledBack) {
                    var message = "Failed to restore an exact inventory after a sell transaction failure for "
                            + player.orElseThrow().uuid();
                    if (failure == null) logger.error(message);
                    else logger.error(message, failure);
                    invocation.errorKey("commands.economy.sell.rollback-failed");
                } else if (failure != null) {
                    invocation.errorKey("service.economy.persistence-failed");
                } else {
                    invocation.error(result.message());
                }
                return;
            }
            invocation.replyKey(
                    "commands.economy.sell.success",
                    Map.of(
                            "count", removedCount,
                            "amount", economy.format(total),
                            "balance", economy.format(result.balance())
                    )
            );
        }));
        return 1;
    }

    private Optional<Integer> parseRequested(CommandInvocation invocation) {
        if (invocation.args().length == 1) return Optional.of(-1);
        try {
            var value = Integer.parseInt(invocation.args()[1]);
            if (value <= 0) throw new NumberFormatException();
            return Optional.of(value);
        } catch (NumberFormatException _) {
            invocation.errorKey("commands.economy.sell.invalid-amount");
            return Optional.empty();
        }
    }

    private List<SaleLine> select(CommandInvocation invocation, CellPlayer player, int requested) {
        var selector = invocation.args()[0].toLowerCase(Locale.ROOT);
        if (selector.equals("all") && requested > 0) {
            invocation.errorKey("commands.economy.sell.amount-not-allowed-for-all");
            return List.of();
        }

        var snapshots = selector.equals("hand")
                ? platform.heldInventorySnapshot(player).map(List::of).orElseGet(List::of)
                : platform.inventorySnapshot(player).orElseGet(List::of);
        if (snapshots.isEmpty()) {
            invocation.errorKey(selector.equals("hand")
                    ? "commands.economy.sell.empty-hand"
                    : "commands.economy.sell.no-sellable-items");
            return List.of();
        }

        Optional<String> requestedItem = Optional.empty();
        if (!selector.equals("all") && !selector.equals("hand")) {
            requestedItem = items.parse(selector).map(ItemDescriptor::normalizedItem);
            if (requestedItem.isEmpty()) {
                invocation.errorKey("commands.economy.sell.invalid-item", Map.of("item", selector));
                return List.of();
            }
        }

        var result = new ArrayList<SaleLine>();
        var remaining = requested;
        for (var snapshot : snapshots) {
            if (!platform.plainInventoryItem(snapshot)) {
                invocation.errorKey("commands.economy.component-item-unsupported");
                return List.of();
            }
            var descriptor = platform.describeInventoryItem(snapshot);
            if (descriptor.isEmpty()) {
                invocation.errorKey("commands.economy.sell.inventory-changed");
                return List.of();
            }
            var item = descriptor.orElseThrow();
            if (requestedItem.isPresent() && !item.normalizedItem().equals(requestedItem.orElseThrow())) continue;
            var worth = worths.worth(item.normalizedItem());
            if (worth.isEmpty() || worth.orElseThrow().signum() <= 0) continue;

            var count = remaining > 0 ? Math.min(remaining, item.count) : item.count;
            if (count <= 0) continue;
            result.add(new SaleLine(new InventoryStackSelection(snapshot, count), worth.orElseThrow()));
            if (remaining > 0) {
                remaining -= count;
                if (remaining == 0) break;
            }
        }

        if (result.isEmpty()) {
            invocation.errorKey(requestedItem.isPresent()
                            ? "commands.economy.worth-missing"
                            : "commands.economy.sell.no-sellable-items",
                    requestedItem.<Map<String, Object>>map(item -> Map.of("item", item)).orElseGet(Map::of));
            return List.of();
        }
        if (remaining > 0) {
            invocation.errorKey(
                    "commands.economy.sell.not-enough",
                    Map.of("requested", requested, "available", requested - remaining)
            );
            return List.of();
        }
        return List.copyOf(result);
    }

    private record SaleLine(
            InventoryStackSelection selection,
            BigDecimal unitWorth
    ) {

    }

}
