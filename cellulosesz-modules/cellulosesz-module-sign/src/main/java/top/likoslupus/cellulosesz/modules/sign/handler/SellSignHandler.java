package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class SellSignHandler extends AbstractTradeSignHandler implements CellSignHandler {

    private final EconomyService economy;
    private final PlatformService platform;
    private final CellulosesZLogger logger;

    public SellSignHandler(
            ItemService items,
            EconomyService economy,
            PlatformService platform,
            CellulosesZLogger logger
    ) {
        super(items);
        this.economy = economy;
        this.platform = platform;
        this.logger = logger;
    }

    @Override
    public String id() {
        return "Sell";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);
        if (descriptor.isEmpty() || price.isEmpty() || !items.valid(descriptor.orElseThrow())) {
            return SignUseResult.failure("service.sign.sell-format");
        }
        return SignUseResult.success("service.sign.valid");
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);
        if (descriptor.isEmpty() || price.isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure("service.sign.sell-format"));
        }

        var request = new InventoryItemRequest(
                items.commandArgument(descriptor.orElseThrow()), descriptor.orElseThrow().count
        );
        var mutation = platform.prepareInventoryExchange(context.player(), List.of(request), List.of());
        if (mutation.isEmpty() || !mutation.orElseThrow().commit()) {
            return CompletableFuture.completedFuture(SignUseResult.failure("service.sign.sell-not-enough"));
        }

        return economy.deposit(
                context.player().uuid(),
                price.orElseThrow(),
                TransactionCause.command(
                        context.player().name(),
                        "sell sign " + descriptor.orElseThrow().normalizedItem()
                )
        ).thenCompose(deposit -> {
            if (deposit.success()) {
                return CompletableFuture.completedFuture(SignUseResult.success(
                        "service.sign.sell-success",
                        Map.of(
                                "count", descriptor.orElseThrow().count,
                                "item", descriptor.orElseThrow().normalizedItem(),
                                "price", economy.format(price.orElseThrow())
                        )
                ));
            }
            return platform.callOnServerThread(mutation.orElseThrow()::rollback)
                    .thenApply(rolledBack -> {
                        if (!rolledBack) {
                            logger.error("Failed to restore exact inventory after Sell sign failure for "
                                    + context.player().uuid());
                            return SignUseResult.failure("service.sign.sell-rollback-failed");
                        }
                        return SignUseResult.failure(deposit.message());
                    });
        }).exceptionallyCompose(failure -> platform.callOnServerThread(mutation.orElseThrow()::rollback)
                .thenApply(rolledBack -> {
                    if (!rolledBack) {
                        logger.error("Failed to restore exact inventory after exceptional Sell sign failure for "
                                + context.player().uuid(), failure);
                        return SignUseResult.failure("service.sign.sell-rollback-failed");
                    }
                    return SignUseResult.failure("service.sign.execution-failed");
                }));
    }

}
