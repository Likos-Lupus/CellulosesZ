package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.InventoryMutation;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class SellSignHandler extends AbstractTradeSignHandler implements CellSignHandler {

    private final EconomyService economy;
    private final InventoryPlatformService inventory;
    private final ServerThreadExecutor serverThread;
    private final CellulosesZLogger logger;

    public SellSignHandler(
            ItemService items,
            EconomyService economy,
            InventoryPlatformService inventory,
            ServerThreadExecutor serverThread,
            CellulosesZLogger logger
    ) {
        super(items);
        this.economy = requireNonNull(economy, "economy");
        this.inventory = requireNonNull(inventory, "inventory");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public String id() {
        return "Sell";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);

        if (descriptor.isEmpty()
                || price.isEmpty()
                || !items.valid(descriptor.orElseThrow())
        ) {
            return SignUseResult.failure("service.sign.sell-format");
        }

        return SignUseResult.success("service.sign.valid");
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);

        if (descriptor.isEmpty() || price.isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.sell-format"
            ));
        }

        var request = new InventoryItemRequest(
                descriptor.orElseThrow().normalizedArgument(),
                descriptor.orElseThrow().count
        );
        var prepared = inventory.prepareExchange(
                context.player(),
                List.of(request),
                List.of()
        );

        if (!prepared.successful() || prepared.value().isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.sell-not-enough"
            ));
        }

        var mutation = prepared.value().orElseThrow();
        if (!mutation.commit()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.sell-not-enough"
            ));
        }

        return economy
                .deposit(
                        context.player().uuid(),
                        price.orElseThrow(),
                        TransactionCause.command(
                                context.player().name(),
                                "sell sign " + descriptor.orElseThrow().normalizedItem()
                        )
                )
                .thenCompose(deposit -> {
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

                    return rollback(context, mutation, null)
                            .thenApply(rolledBack -> rolledBack
                                    ? SignUseResult.failure(deposit.message())
                                    : SignUseResult.failure("service.sign.sell-rollback-failed")
                            );
                })
                .exceptionallyCompose(failure -> rollback(context, mutation, failure)
                        .thenApply(rolledBack -> rolledBack
                                ? SignUseResult.failure("service.sign.execution-failed")
                                : SignUseResult.failure("service.sign.sell-rollback-failed")
                        )
                );
    }

    private CompletableFuture<Boolean> rollback(
            SignUseContext context,
            InventoryMutation mutation,
            @Nullable Throwable failure
    ) {
        return serverThread
                .submit(mutation::rollback)
                .thenApply(rolledBack -> {
                    if (!rolledBack) {
                        var message =
                                "Failed to restore exact inventory after Sell sign failure for "
                                        + context.player().uuid();

                        if (failure == null) {
                            logger.error(message);
                        } else {
                            logger.error(message, failure);
                        }
                    }
                    return rolledBack;
                });
    }

}
