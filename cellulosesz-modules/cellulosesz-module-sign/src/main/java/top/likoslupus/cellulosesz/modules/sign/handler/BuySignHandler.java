package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class BuySignHandler extends AbstractTradeSignHandler implements CellSignHandler {

    private final EconomyService economy;
    private final InventoryPlatformService inventory;
    private final ServerThreadExecutor serverThread;

    public BuySignHandler(
            ItemService items,
            EconomyService economy,
            InventoryPlatformService inventory,
            ServerThreadExecutor serverThread
    ) {
        super(items);
        this.economy = requireNonNull(economy, "economy");
        this.inventory = requireNonNull(inventory, "inventory");
        this.serverThread = requireNonNull(serverThread, "serverThread");
    }

    @Override
    public String id() {
        return "Buy";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);

        if (descriptor.isEmpty()
                || price.isEmpty()
                || !items.valid(descriptor.orElseThrow())
        ) {
            return SignUseResult.failure("service.sign.buy-format");
        }

        if (items.blacklisted(descriptor.orElseThrow())) {
            return SignUseResult.failure(
                    "service.sign.item-blacklisted",
                    Map.of("item", descriptor.orElseThrow().normalizedItem())
            );
        }

        return SignUseResult.success("service.sign.valid");
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);

        if (descriptor.isEmpty() || price.isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.buy-format"
            ));
        }

        var request = new InventoryItemRequest(
                descriptor.orElseThrow().normalizedArgument(),
                descriptor.orElseThrow().count
        );
        var prepared = inventory.prepareExchange(
                context.player(),
                List.of(),
                List.of(request)
        );

        if (!prepared.successful() || prepared.value().isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.buy-inventory-full"
            ));
        }

        var mutation = prepared.value().orElseThrow();
        var cause = TransactionCause.command(
                context.player().name(),
                "buy sign " + descriptor.orElseThrow().normalizedItem()
        );

        return economy
                .withdraw(
                        context.player().uuid(),
                        price.orElseThrow(),
                        cause
                )
                .thenCompose(withdrawal -> {
                    if (!withdrawal.success()) {
                        return CompletableFuture.completedFuture(SignUseResult.failure(withdrawal.message()));
                    }

                    return serverThread
                            .submit(mutation::commit)
                            .thenCompose(committed -> {
                                if (committed) {
                                    return CompletableFuture.completedFuture(SignUseResult.success(
                                            "service.sign.buy-success",
                                            Map.of(
                                                    "count",
                                                    descriptor.orElseThrow().count,
                                                    "item",
                                                    descriptor.orElseThrow().normalizedItem(),
                                                    "price",
                                                    economy.format(price.orElseThrow())
                                            )
                                    ));
                                }

                                return economy
                                        .deposit(
                                                context.player().uuid(),
                                                price.orElseThrow(),
                                                TransactionCause.system("buy sign refund")
                                        )
                                        .handle((refund, failure) ->
                                                failure == null && refund.success()
                                                        ?
                                                        SignUseResult.failure(
                                                                "service.sign.buy-inventory-full"
                                                        )
                                                        : SignUseResult.failure(
                                                                "service.sign.buy-rollback-failed"
                                                        ));
                            });
                })
                .exceptionally(_ -> SignUseResult.failure("service.sign.execution-failed"));
    }

}
