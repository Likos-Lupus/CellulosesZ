package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;

import java.math.BigDecimal;
import java.util.Optional;

abstract class AbstractTradeSignHandler {

    protected final ItemService items;

    AbstractTradeSignHandler(ItemService items) {
        this.items = items;
    }

    protected PlatformResult<ItemDescriptor> item(SignUseContext context) {
        var count = context.line(1);
        var descriptor = context.line(2);
        if (count.isBlank() || descriptor.isBlank()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_INPUT,
                    "Trade sign item and count must not be blank"
            );
        }
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
