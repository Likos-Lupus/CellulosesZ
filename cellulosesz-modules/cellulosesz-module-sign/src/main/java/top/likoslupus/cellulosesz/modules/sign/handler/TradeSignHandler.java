package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.common.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TradeSignHandler implements SynchronousSignHandler {

    private final ItemService items;
    private final InventoryPlatformService inventory;

    public TradeSignHandler(
            ItemService items,
            InventoryPlatformService inventory
    ) {
        this.items = requireNonNull(items, "items");
        this.inventory = requireNonNull(inventory, "inventory");
    }

    @Override
    public String id() {
        return "Trade";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var cost = cost(context);
        var costResult = SignHandlerSupport.validateItem(items, cost, "service.sign.trade-format");
        if (!costResult.success()) {
            return costResult;
        }

        var reward = reward(context);
        var rewardResult = SignHandlerSupport.validateItem(
                items,
                reward,
                "service.sign.trade-format"
        );
        if (!rewardResult.success()) {
            return rewardResult;
        }

        return SignUseResult.success("service.sign.valid");
    }

    private PlatformResult<ItemDescriptor> cost(SignUseContext context) {
        return items.parse(context.line(1));
    }

    private PlatformResult<ItemDescriptor> reward(SignUseContext context) {
        return items.parse(context.line(2));
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var costResult = cost(context);
        var rewardResult = reward(context);

        if (!costResult.successful() || costResult.value() == null) {
            return SignHandlerSupport.itemFailure(
                    costResult.status(),
                    "service.sign.trade-format"
            );
        }

        if (!rewardResult.successful() || rewardResult.value() == null) {
            return SignHandlerSupport.itemFailure(
                    rewardResult.status(),
                    "service.sign.trade-format"
            );
        }

        var cost = costResult.value();
        var reward = rewardResult.value();
        var prepared = inventory.prepareExchange(
                context.player(),
                List.of(request(cost)),
                List.of(request(reward))
        );

        if (!prepared.successful() || prepared.value() == null) {
            return SignUseResult.failure("service.sign.trade-inventory-full");
        }

        var committed = prepared.value().commit();
        if (!committed.successful()) {
            return SignUseResult.failure("service.sign.trade-inventory-changed");
        }

        return SignUseResult.success(
                "service.sign.trade-success",
                MessageArguments.builder()
                        .add(cost.normalizedArgument())
                        .add(reward.normalizedArgument())
                        .build()
        );
    }

    private InventoryItemRequest request(ItemDescriptor descriptor) {
        return new InventoryItemRequest(descriptor.normalizedArgument(), descriptor.count());
    }

}
