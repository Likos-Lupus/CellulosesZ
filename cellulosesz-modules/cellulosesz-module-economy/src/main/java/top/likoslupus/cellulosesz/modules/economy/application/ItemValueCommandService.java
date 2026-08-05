package top.likoslupus.cellulosesz.modules.economy.application;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.economy.TransactionResult;
import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class ItemValueCommandService {

    private final InventoryPlatformService inventories;
    private final ItemService items;
    private final WorthService worths;
    private final EconomyService economy;
    private final ServerThreadExecutor serverThread;

    public ItemValueCommandService(
            InventoryPlatformService inventories,
            ItemService items,
            WorthService worths,
            EconomyService economy,
            ServerThreadExecutor serverThread
    ) {
        this.inventories = requireNonNull(inventories, "inventories");
        this.items = requireNonNull(items, "items");
        this.worths = requireNonNull(worths, "worths");
        this.economy = requireNonNull(economy, "economy");
        this.serverThread = requireNonNull(serverThread, "serverThread");
    }

    public Set<String> itemNames() {
        return items.itemNames();
    }

    public CompletableFuture<EconomyCommandResult> worth(
            CellPlayer player,
            WorthSelector selector
    ) {
        requireNonNull(player, "player");
        requireNonNull(selector, "selector");

        return serverThread.submit(() -> switch (selector) {
            case HAND -> {
                var held = inventories.heldSlot(player);
                yield held.successful()
                        ? buildWorth(List.of(held.value().orElseThrow()))
                        : EconomyCommandResult.failure("commands.economy.sell.empty-hand");
            }
            case INVENTORY -> {
                var snapshot = inventories.inventorySlots(player);
                yield snapshot.successful()
                        ? buildWorth(snapshot.value().orElseThrow())
                        : EconomyCommandResult.failure("commands.economy.sell.inventory-changed");
            }
        });
    }

    private EconomyCommandResult buildWorth(List<InventorySlotView> views) {
        var quantities = new LinkedHashMap<String, Long>();
        for (var view : views) {
            if (!view.plain()) {
                return EconomyCommandResult.failure("commands.economy.component-item-unsupported");
            }
            try {
                quantities.merge(
                        view.descriptor().normalizedItem(),
                        (long) view.descriptor().count(),
                        Math::addExact
                );
            } catch (ArithmeticException failure) {
                return EconomyCommandResult.failure("commands.economy.worth-count-overflow");
            }
        }

        if (quantities.isEmpty()) {
            return EconomyCommandResult.failure("commands.economy.sell.no-sellable-items");
        }

        var messages = new ArrayList<LocalizedMessage>();
        var total = BigDecimal.ZERO;
        var found = 0;

        for (var entry : quantities.entrySet()) {
            var unit = worths.worth(entry.getKey());
            if (unit.isEmpty()) {
                messages.add(LocalizedMessage.of(
                        "commands.economy.worth-row-missing",
                        MessageArguments.builder()
                                .add(entry.getKey())
                                .add(entry.getValue())
                                .build()
                ));
                continue;
            }

            var line = unit.orElseThrow().multiply(BigDecimal.valueOf(entry.getValue()));
            total = total.add(line);
            found++;
            messages.add(LocalizedMessage.of(
                    "commands.economy.worth-row",
                    MessageArguments.builder()
                            .add(entry.getKey())
                            .add(entry.getValue())
                            .add(economy.format(line))
                            .build()
            ));
        }

        if (found == 0) {
            return EconomyCommandResult.failure(
                    "commands.economy.worth-missing",
                    MessageArguments.builder()
                            .add(String.join(", ", quantities.keySet()))
                            .build()
            );
        }

        messages.add(LocalizedMessage.of(
                "commands.economy.worth-total",
                MessageArguments.builder()
                        .add(found)
                        .add(economy.format(total))
                        .build()
        ));

        return EconomyCommandResult.success(messages);
    }

    public CompletableFuture<EconomyCommandResult> worthItem(String itemInput, int requested) {
        if (requested <= 0) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.worth.invalid-amount"
            ));
        }

        var parsed = items.parse(itemInput);
        if (!parsed.successful()) {
            return CompletableFuture.completedFuture(itemParseFailure(parsed));
        }

        var canonical = parsed.value().orElseThrow().normalizedItem();
        var unit = worths.worth(canonical);
        if (unit.isEmpty()) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.worth-missing",
                    MessageArguments.builder().add(canonical).build()
            ));
        }

        var amount = unit.orElseThrow().multiply(BigDecimal.valueOf(requested));
        return CompletableFuture.completedFuture(EconomyCommandResult.success(List.of(
                LocalizedMessage.of(
                        "commands.economy.worth-row",
                        MessageArguments.builder()
                                .add(canonical)
                                .add(requested)
                                .add(economy.format(amount))
                                .build()
                ),
                LocalizedMessage.of(
                        "commands.economy.worth-total",
                        MessageArguments.builder()
                                .add(1)
                                .add(economy.format(amount))
                                .build()
                )
        )));
    }

    private static EconomyCommandResult itemParseFailure(PlatformResult<?> result) {
        return switch (result.status()) {
            case INVALID_ARGUMENT, INVALID_INPUT, NOT_FOUND -> EconomyCommandResult.failure(
                    "commands.economy.sell.invalid-item"
            );
            default -> EconomyCommandResult.failed("service.economy.persistence-failed");
        };
    }

    public CompletableFuture<EconomyCommandResult> sell(
            CellPlayer player,
            SellSelector selector,
            Optional<String> itemInput,
            int requested
    ) {
        requireNonNull(player, "player");
        requireNonNull(selector, "selector");
        requireNonNull(itemInput, "itemInput");

        return serverThread
                .submit(() -> prepareSale(player, selector, itemInput, requested))
                .thenCompose(prepared -> {
                    if (prepared.failure().isPresent()) {
                        return CompletableFuture.completedFuture(prepared.failure().orElseThrow());
                    }

                    var transaction = prepared.transaction().orElseThrow();
                    return economy.deposit(
                                    player.uuid(),
                                    prepared.total(),
                                    TransactionCause.command(player.name(), "sell")
                            )
                            .handle(DepositOutcome::new)
                            .thenCompose(outcome -> {
                                if (outcome.failure() == null
                                        && outcome.result() != null
                                        && outcome.result().success()
                                ) {
                                    return CompletableFuture.completedFuture(EconomyCommandResult.success(
                                            "commands.economy.sell.success",
                                            MessageArguments.builder()
                                                    .add(prepared.count())
                                                    .add(economy.format(prepared.total()))
                                                    .build()
                                    ));
                                }

                                return serverThread
                                        .submit(transaction::rollback)
                                        .thenApply(rolledBack -> {
                                            if (!rolledBack.successful()) {
                                                return EconomyCommandResult.failed(
                                                        "commands.economy.sell.rollback-failed"
                                                );
                                            }

                                            if (outcome.failure() != null) {
                                                return EconomyCommandResult.failure(
                                                        "service.economy.persistence-failed"
                                                );
                                            }

                                            return EconomyCommandResult.failure(
                                                    requireNonNull(
                                                            outcome.result(),
                                                            "result"
                                                    ).message()
                                            );
                                        });
                            });
                });
    }

    private SalePreparation prepareSale(
            CellPlayer player,
            SellSelector selector,
            Optional<String> itemInput,
            int requested
    ) {
        var snapshot = inventories.inventorySlots(player);
        if (!snapshot.successful()) {
            return SalePreparation.failure(EconomyCommandResult.failure(
                    "commands.economy.sell.inventory-changed"
            ));
        }

        var all = snapshot.value().orElseThrow();
        final List<InventorySlotView> candidates;

        switch (selector) {
            case HAND -> {
                var held = inventories.heldSlot(player);
                if (!held.successful()) {
                    return SalePreparation.failure(EconomyCommandResult.failure(
                            "commands.economy.sell.empty-hand"
                    ));
                }

                candidates = List.of(held.value().orElseThrow());
            }
            case ITEM -> {
                var input = itemInput.orElseThrow();
                var parsed = items.parse(input);
                if (!parsed.successful()) {
                    return SalePreparation.failure(itemParseFailure(parsed));
                }

                var canonical = parsed.value().orElseThrow().normalizedItem();

                candidates = all.stream()
                        .filter(view ->
                                view.descriptor().normalizedItem().equals(canonical)
                        )
                        .toList();
            }
            default -> candidates = all;
        }

        var remaining = selector == SellSelector.ALL
                ? Integer.MAX_VALUE
                : requested;
        var selections = new ArrayList<InventoryStackSelection>();
        var total = BigDecimal.ZERO;
        var count = 0;

        for (var view : candidates) {
            if (!view.plain()) {
                return SalePreparation.failure(EconomyCommandResult.failure(
                        "commands.economy.component-item-unsupported"
                ));
            }

            var worth = worths.worth(view.descriptor().normalizedItem());
            if (worth.isEmpty()) {
                continue;
            }

            var selected = selector == SellSelector.ALL
                    ? view.descriptor().count()
                    : Math.min(remaining, view.descriptor().count());
            if (selected <= 0) {
                continue;
            }

            selections.add(new InventoryStackSelection(view.snapshot(), selected));
            total = total.add(worth.orElseThrow().multiply(BigDecimal.valueOf(selected)));
            try {
                count = Math.addExact(count, selected);
            } catch (ArithmeticException failure) {
                return SalePreparation.failure(EconomyCommandResult.failure(
                        "commands.economy.worth-count-overflow"
                ));
            }

            if (selector != SellSelector.ALL) {
                remaining -= selected;
                if (remaining == 0) {
                    break;
                }
            }
        }

        if (selections.isEmpty()) {
            return SalePreparation.failure(EconomyCommandResult.failure(
                    selector == SellSelector.ITEM
                            ? "commands.economy.worth-missing"
                            : "commands.economy.sell.no-sellable-items",
                    itemInput.map(_ -> MessageArguments.empty())
                            .orElseGet(MessageArguments::empty)
            ));
        }

        if (selector != SellSelector.ALL && remaining > 0) {
            return SalePreparation.failure(EconomyCommandResult.failure(
                    "commands.economy.sell.not-enough",
                    MessageArguments.builder()
                            .add(requested - remaining)
                            .add(itemInput.orElse("items"))
                            .add(requested)
                            .build()
            ));
        }

        var prepared = inventories.prepareRemoval(player, selections);
        if (!prepared.successful() || prepared.value().isEmpty()) {
            return SalePreparation.failure(EconomyCommandResult.failure(
                    "commands.economy.sell.inventory-changed"
            ));
        }

        var mutation = prepared.value().orElseThrow();
        var committed = mutation.commit();
        if (!committed.successful()) {
            return SalePreparation.failure(EconomyCommandResult.failed(
                    "commands.economy.sell.inventory-changed",
                    MessageArguments.empty()
            ));
        }

        return SalePreparation.success(mutation, total, count);
    }

    public CompletableFuture<EconomyCommandResult> setWorth(String input, BigDecimal amount) {
        var parsed = items.parse(input);
        if (!parsed.successful()) {
            return CompletableFuture.completedFuture(itemParseFailure(parsed));
        }

        var canonical = parsed.value().orElseThrow().normalizedItem();
        return worths
                .setWorth(canonical, amount)
                .thenApply(_ -> EconomyCommandResult.success(
                        "commands.economy.set-worth-command.reply.set-worth",
                        MessageArguments.builder()
                                .add(canonical)
                                .add(amount.stripTrailingZeros().toPlainString())
                                .build()
                ));
    }

    public CompletableFuture<EconomyCommandResult> removeWorth(String input) {
        var parsed = items.parse(input);
        if (!parsed.successful()) {
            return CompletableFuture.completedFuture(itemParseFailure(parsed));
        }

        var canonical = parsed.value().orElseThrow().normalizedItem();
        return worths
                .removeWorth(canonical)
                .thenApply(removed ->
                        removed
                                ?
                                EconomyCommandResult.success(
                                        "commands.economy.worth-removed",
                                        MessageArguments.builder().add(canonical).build()
                                )
                                : EconomyCommandResult.failure(
                                        "commands.economy.worth-missing",
                                        MessageArguments.builder().add(canonical).build()
                                )
                );
    }

    public enum SellSelector {

        HAND,
        ALL,
        ITEM

    }

    public enum WorthSelector {

        HAND,
        INVENTORY

    }

    private record DepositOutcome(
            @Nullable TransactionResult result,
            @Nullable Throwable failure
    ) {

    }

    private record SalePreparation(
            Optional<InventoryMutation> transaction,
            BigDecimal total,
            int count,
            Optional<EconomyCommandResult> failure
    ) {

        static SalePreparation success(
                InventoryMutation mutation,
                BigDecimal total,
                int count
        ) {
            return new SalePreparation(
                    Optional.of(mutation),
                    total,
                    count,
                    Optional.empty()
            );
        }

        static SalePreparation failure(EconomyCommandResult result) {
            return new SalePreparation(
                    Optional.empty(),
                    BigDecimal.ZERO,
                    0,
                    Optional.of(result)
            );
        }

    }

}
