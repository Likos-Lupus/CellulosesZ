package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

import java.util.Map;

public final class SellSignHandler extends AbstractTradeSignHandler {

    private final EconomyService economy;

    public SellSignHandler(
            ItemService items,
            EconomyService economy
    ) {
        super(items);
        this.economy = economy;
    }

    @Override
    public String id() {
        return "Sell";
    }

    @Override
    public SignUseResult use(SignUseContext context) {
        var descriptor = item(context);
        var price = price(context);

        if (descriptor.isEmpty() || price.isEmpty()) {
            return SignUseResult.failure("service.sign.sell-format");
        }

        if (items.count(context.player(), descriptor.get()) < descriptor.get().count) {
            return SignUseResult.failure("service.sign.sell-not-enough");
        }

        if (!items.take(context.player(), descriptor.get())) {
            return SignUseResult.failure("service.sign.sell-take-failed");
        }

        var deposit = economy.deposit(
                context.player().uuid(),
                price.get(),
                TransactionCause.command(
                        context.player().name(),
                        "sell sign %s".formatted(descriptor.get().normalizedItem())
                )
        );
        if (!deposit.success()) {
            items.give(context.player(), descriptor.get());
            return SignUseResult.failure(deposit.message());
        }

        return SignUseResult.success(
                "service.sign.sell-success",
                Map.of(
                        "count", descriptor.get().count,
                        "item", descriptor.get().normalizedItem(),
                        "price", price.get().toPlainString()
                )
        );
    }

}
