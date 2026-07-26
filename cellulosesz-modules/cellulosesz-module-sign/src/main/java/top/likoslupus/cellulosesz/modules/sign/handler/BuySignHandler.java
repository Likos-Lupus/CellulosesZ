package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class BuySignHandler extends AbstractTradeSignHandler implements CellSignHandler {

    private final EconomyService economy;
    private final PlatformService platform;

    public BuySignHandler(
            ItemService items,
            EconomyService economy,
            PlatformService platform
    ) {
        super(items);
        this.economy = economy;
        this.platform = platform;
    }

    @Override
    public String id() {
        return "Buy";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);
        if (descriptor.isEmpty() || price.isEmpty() || !items.valid(descriptor.orElseThrow())) {
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
            return CompletableFuture.completedFuture(SignUseResult.failure("service.sign.buy-format"));
        }

        var request = new InventoryItemRequest(
                items.commandArgument(descriptor.orElseThrow()), descriptor.orElseThrow().count
        );
        var mutation = platform.prepareInventoryExchange(context.player(), List.of(), List.of(request));
        if (mutation.isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure("service.sign.buy-inventory-full"));
        }

        var cause = TransactionCause.command(
                context.player().name(), "buy sign " + descriptor.orElseThrow().normalizedItem()
        );
        return economy.withdraw(context.player().uuid(), price.orElseThrow(), cause)
                .thenCompose(withdrawal -> {
                    if (!withdrawal.success()) {
                        return CompletableFuture.completedFuture(SignUseResult.failure(withdrawal.message()));
                    }
                    return platform.callOnServerThread(mutation.orElseThrow()::commit)
                            .handle((committed, commitFailure) ->
                                    commitFailure == null && Boolean.TRUE.equals(committed))
                            .thenCompose(committed -> {
                                if (committed) {
                                    return CompletableFuture.completedFuture(SignUseResult.success(
                                            "service.sign.buy-success",
                                            Map.of(
                                                    "count", descriptor.orElseThrow().count,
                                                    "item", descriptor.orElseThrow().normalizedItem(),
                                                    "price", economy.format(price.orElseThrow())
                                            )
                                    ));
                                }
                                return economy.deposit(
                                        context.player().uuid(),
                                        price.orElseThrow(),
                                        TransactionCause.system("buy sign refund")
                                ).handle((refund, refundFailure) ->
                                        refundFailure == null && refund.success()
                                                ? SignUseResult.failure("service.sign.buy-inventory-full")
                                                : SignUseResult.failure("service.sign.buy-rollback-failed"));
                            });
                })
                .exceptionally(_ -> SignUseResult.failure("service.sign.execution-failed"));
    }

}
