package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;

import java.math.BigDecimal;
import java.util.Optional;

abstract class AbstractTradeSignHandler implements CellSignHandler {

    protected final ItemService items;

    AbstractTradeSignHandler(ItemService items) {
        this.items = items;
    }

    protected Optional<ItemDescriptor> item(SignUseContext context) {
        var count = context.line(1);
        var descriptor = context.line(2);

        if (count.isBlank() || descriptor.isBlank()) return Optional.empty();
        return items.parse(descriptor + " " + count);
    }

    protected Optional<BigDecimal> price(SignUseContext context) {
        var value = context.line(3)
                .replace(",", "")
                .replace("$", "").trim();
        try {
            var price = new BigDecimal(value);
            return price.signum() > 0
                    ? Optional.of(price)
                    : Optional.empty();
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

}
