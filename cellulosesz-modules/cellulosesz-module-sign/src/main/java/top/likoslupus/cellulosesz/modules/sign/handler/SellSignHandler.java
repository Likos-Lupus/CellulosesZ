package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.InventoryMutation;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.common.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

import java.util.List;
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
        var itemResult = SignHandlerSupport.validateItem(
                items,
                descriptor,
                "service.sign.sell-format"
        );

        if (!itemResult.success() || price(context).isEmpty()) {
            return itemResult.success()
                    ? SignUseResult.failure("service.sign.sell-format")
                    : itemResult;
        }

        return SignUseResult.success("service.sign.valid");
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var descriptorResult = item(context);
        var price = price(context);

        if (!descriptorResult.successful()
                || descriptorResult.value() == null
                || price.isEmpty()
        ) {
            return CompletableFuture.completedFuture(
                    SignHandlerSupport.itemFailure(
                            descriptorResult.status(),
                            "service.sign.sell-format"
                    )
            );
        }

        var descriptor = descriptorResult.value();
        var prepared = inventory.prepareExchange(
                context.player(),
                List.of(new InventoryItemRequest(
                        descriptor.normalizedArgument(),
                        descriptor.count()
                )),
                List.of()
        );

        if (!prepared.successful() || prepared.value() == null) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.sell-not-enough"
            ));
        }

        var mutation = prepared.value();

        var committed = mutation.commit();
        if (!committed.successful()) {
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
                                "sell sign " + descriptor.normalizedItem()
                        )
                )
                .thenCompose(deposit -> {
                    if (deposit.success()) {
                        return CompletableFuture.completedFuture(SignUseResult.success(
                                "service.sign.sell-success",
                                MessageArguments.builder()
                                        .add(descriptor.count())
                                        .add(descriptor.normalizedItem())
                                        .add(economy.format(price.orElseThrow()))
                                        .build()
                        ));
                    }

                    return rollback(
                            context,
                            mutation,
                            null
                    ).thenApply(rollback -> rollback.successful()
                            ? SignUseResult.failure(deposit.message())
                            : SignUseResult.failure("service.sign.sell-rollback-failed")
                    );
                })
                .exceptionallyCompose(failure -> rollback(
                                context,
                                mutation,
                                failure
                        ).thenApply(rollback -> rollback.successful()
                                ?
                                SignUseResult.failure(
                                        "service.sign.execution-failed",
                                        MessageArguments.builder()
                                                .add(failure.getClass().getSimpleName())
                                                .build()
                                )
                                : SignUseResult.failure("service.sign.sell-rollback-failed"))
                );
    }

    private CompletableFuture<PlatformResult<Void>> rollback(
            SignUseContext context,
            InventoryMutation mutation,
            @Nullable Throwable failure
    ) {
        return serverThread.submit(mutation::rollback).thenApply(result -> {
            if (!result.successful()) {
                var message = "Failed to restore exact inventory after Sell sign failure for %s: %s".formatted(
                        context.player().uuid(),
                        result.detail()
                );

                if (failure == null) {
                    logger.error(message);
                } else {
                    logger.error(message, failure);
                }
            }

            return result;
        });
    }

}
