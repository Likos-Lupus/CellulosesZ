package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

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
            return SignUseResult.failure("[Sell] 格式: 第二行数量，第三行物品，第四行价格。");
        }

        if (items.count(context.player(), descriptor.get()) < descriptor.get().count) {
            return SignUseResult.failure("物品数量不足。");
        }

        if (!items.take(context.player(), descriptor.get())) {
            return SignUseResult.failure("无法从背包扣除物品。");
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

        return SignUseResult.success("出售成功: %d × %s，获得 %s。".formatted(
                descriptor.get().count,
                descriptor.get().normalizedItem(),
                price.get().toPlainString()
        ));
    }

}
