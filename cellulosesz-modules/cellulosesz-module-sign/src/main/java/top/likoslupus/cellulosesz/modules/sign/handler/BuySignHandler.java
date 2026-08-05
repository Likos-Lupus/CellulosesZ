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
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.util.List;
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
        var itemResult = SignHandlerSupport.validateItem(
                items,
                descriptor,
                "service.sign.buy-format"
        );
        if (!itemResult.success() || price(context).isEmpty()) {
            return itemResult.success()
                    ? SignUseResult.failure("service.sign.buy-format")
                    : itemResult;
        }
        return SignUseResult.success("service.sign.valid");
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var descriptorResult = item(context);
        var price = price(context);
        if (!descriptorResult.successful()
                || descriptorResult.value().isEmpty()
                || price.isEmpty()
        ) {
            return CompletableFuture.completedFuture(
                    SignHandlerSupport.itemFailure(
                            descriptorResult.status(),
                            "service.sign.buy-format"
                    )
            );
        }
        var descriptor = descriptorResult.value().orElseThrow();
        var prepared = inventory.prepareExchange(
                context.player(),
                List.of(),
                List.of(new InventoryItemRequest(
                        descriptor.normalizedArgument(),
                        descriptor.count()
                ))
        );
        if (!prepared.successful() || prepared.value().isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.buy-inventory-full"));
        }

        var mutation = prepared.value().orElseThrow();
        var cause = TransactionCause.command(
                context.player().name(),
                "buy sign " + descriptor.normalizedItem()
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
                                if (committed.successful()) {
                                    return CompletableFuture.completedFuture(SignUseResult.success(
                                            "service.sign.buy-success",
                                            MessageArguments.builder()
                                                    .add(descriptor.count())
                                                    .add(descriptor.normalizedItem())
                                                    .add(economy.format(price.orElseThrow()))
                                                    .build()
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
                .exceptionally(failure -> SignUseResult.failure(
                        "service.sign.execution-failed",
                        MessageArguments.builder()
                                .add(failure.getClass().getSimpleName())
                                .build()
                ));
    }

}
