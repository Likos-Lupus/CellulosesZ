package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;

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
            return SignUseResult.failure("[Buy] 格式: 第二行数量，第三行物品，第四行价格。");
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
            return SignUseResult.failure("背包无法接收物品，交易已退款。");
        }

        return SignUseResult.success("购买成功: %d × %s，花费 %s。".formatted(
                descriptor.get().count,
                descriptor.get().normalizedItem(),
                price.get().toPlainString()
        ));
    }

}
