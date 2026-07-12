package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.Map;

public final class BuySignHandler extends AbstractTradeSignHandler {

    private final EconomyService economy;

    public BuySignHandler(
            ItemService items,
            EconomyService economy
    ) {
        super(items);
        this.economy = economy;
    }

    @Override
    public String id() {
        return "Buy";
    }

    @Override
    public SignUseResult use(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);
        if (descriptor.isEmpty() || price.isEmpty()) {
            return SignUseResult.failure("service.sign.buy-format");
        }

        var cause = TransactionCause.command(
                context.player().name(),
                "buy sign " + descriptor.get().normalizedItem()
        );
        var withdrawal = economy.withdraw(
                context.player().uuid(),
                price.get(),
                cause
        );
        if (!withdrawal.success()) return SignUseResult.failure(withdrawal.message());

        if (!items.give(context.player(), descriptor.get())) {
            economy.deposit(
                    context.player().uuid(),
                    price.get(),
                    TransactionCause.system("refund failed buy sign")
            );
            return SignUseResult.failure("service.sign.buy-inventory-full");
        }

        return SignUseResult.success(
                "service.sign.buy-success",
                Map.of(
                        "count", descriptor.get().count,
                        "item", descriptor.get().normalizedItem(),
                        "price", price.get().toPlainString()
                )
        );
    }

}
