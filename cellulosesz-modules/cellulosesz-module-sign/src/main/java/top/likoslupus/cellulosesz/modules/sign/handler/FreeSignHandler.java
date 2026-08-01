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

public final class FreeSignHandler implements SynchronousSignHandler {

    private final ItemService items;
    private final InventoryPlatformService inventory;

    public FreeSignHandler(
            ItemService items,
            InventoryPlatformService inventory
    ) {
        this.items = requireNonNull(items, "items");
        this.inventory = requireNonNull(inventory, "inventory");
    }

    @Override
    public String id() {
        return "Free";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var item = item(context);
        var result = SignHandlerSupport.validateItem(
                items,
                item,
                "service.sign.free-format"
        );

        if (!result.success()) {
            return result;
        }

        if (items.blacklisted(item.orElseThrow())) {
            return SignUseResult.failure(
                    "service.sign.item-blacklisted",
                    Map.of("item", item.orElseThrow().normalizedItem())
            );
        }

        if (item.orElseThrow().count > items.maxStackSize(item.orElseThrow())) {
            return SignUseResult.failure("service.sign.free-stack-limit");
        }

        return SignUseResult.success("service.sign.valid");
    }

    private Optional<ItemDescriptor> item(SignUseContext context) {
        var count = SignHandlerSupport.count(
                context.line(1),
                1,
                64
        );

        if (count.isEmpty() || context.line(2).isBlank()) {
            return Optional.empty();
        }

        return items.parse(context.line(2) + " " + count.orElseThrow());
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var item = item(context);
        if (item.isEmpty()) {
            return SignUseResult.failure("service.sign.free-format");
        }

        var request = new InventoryItemRequest(
                items.commandArgument(item.orElseThrow()),
                item.orElseThrow().count
        );
        var prepared = inventory.prepareExchange(
                context.player(),
                List.of(),
                List.of(request)
        );
        if (!prepared.successful()
                || prepared.value().isEmpty()
                || !prepared.value().orElseThrow().commit()
        ) {
            return SignUseResult.failure("service.sign.free-inventory-full");
        }

        return SignUseResult.success(
                "service.sign.free-success",
                SignHandlerSupport.itemPlaceholders(item.orElseThrow())
        );
    }

}
