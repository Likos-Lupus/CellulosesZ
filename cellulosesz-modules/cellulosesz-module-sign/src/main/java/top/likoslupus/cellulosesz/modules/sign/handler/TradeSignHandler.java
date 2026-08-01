package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        var reward = reward(context);
        var costResult = SignHandlerSupport.validateItem(
                items,
                cost,
                "service.sign.trade-format"
        );

        if (!costResult.success()) {
            return costResult;
        }

        var rewardResult = SignHandlerSupport.validateItem(
                items,
                reward,
                "service.sign.trade-format"
        );

        if (!rewardResult.success()) {
            return rewardResult;
        }

        if (items.blacklisted(reward.orElseThrow())) {
            return SignUseResult.failure(
                    "service.sign.item-blacklisted",
                    Map.of("item", reward.orElseThrow().normalizedItem())
            );
        }

        return SignUseResult.success("service.sign.valid");
    }

    private Optional<ItemDescriptor> cost(SignUseContext context) {
        return items.parse(context.line(1));
    }

    private Optional<ItemDescriptor> reward(SignUseContext context) {
        return items.parse(context.line(2));
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var cost = cost(context);
        var reward = reward(context);

        if (cost.isEmpty() || reward.isEmpty()) {
            return SignUseResult.failure("service.sign.trade-format");
        }

        var prepared = inventory.prepareExchange(
                context.player(),
                List.of(request(cost.orElseThrow())),
                List.of(request(reward.orElseThrow()))
        );

        if (!prepared.successful() || prepared.value().isEmpty()) {
            return SignUseResult.failure("service.sign.trade-inventory-full");
        }

        if (!prepared.value().orElseThrow().commit()) {
            return SignUseResult.failure("service.sign.trade-inventory-changed");
        }

        return SignUseResult.success(
                "service.sign.trade-success",
                Map.of(
                        "cost", items.commandArgument(cost.orElseThrow()),
                        "reward", items.commandArgument(reward.orElseThrow())
                )
        );
    }

    private InventoryItemRequest request(ItemDescriptor descriptor) {
        return new InventoryItemRequest(
                items.commandArgument(descriptor),
                descriptor.count
        );
    }

}
