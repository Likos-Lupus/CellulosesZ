package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.List;

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
        var parsed = item(context);

        var result = SignHandlerSupport.validateItem(
                items,
                parsed,
                "service.sign.free-format"
        );
        if (!result.success()) {
            return result;
        }

        var descriptor = parsed.value().orElseThrow();
        var maximum = items.maxStackSize(descriptor);

        if (!maximum.successful() || maximum.value().isEmpty()) {
            return SignHandlerSupport.itemFailure(
                    maximum.status(),
                    "service.sign.free-format"
            );
        }

        if (descriptor.count() > maximum.value().orElseThrow()) {
            return SignUseResult.failure("service.sign.free-stack-limit");
        }

        return SignUseResult.success("service.sign.valid");
    }

    private PlatformResult<ItemDescriptor> item(SignUseContext context) {
        var count = SignHandlerSupport.count(
                context.line(1),
                1,
                64
        );

        if (count.isEmpty() || context.line(2).isBlank()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_INPUT,
                    "Free sign item and count are invalid"
            );
        }

        return items.parse(context.line(2) + " " + count.orElseThrow());
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var parsed = item(context);

        if (!parsed.successful() || parsed.value().isEmpty()) {
            return SignHandlerSupport.itemFailure(
                    parsed.status(),
                    "service.sign.free-format"
            );
        }

        var descriptor = parsed.value().orElseThrow();
        var prepared = inventory.prepareExchange(
                context.player(),
                List.of(),
                List.of(new InventoryItemRequest(
                        descriptor.normalizedArgument(),
                        descriptor.count()
                ))
        );

        if (!prepared.successful() || prepared.value().isEmpty()) {
            return SignUseResult.failure("service.sign.free-inventory-full");
        }

        var committed = prepared.value().orElseThrow().commit();
        if (!committed.successful()) {
            return SignUseResult.failure("service.sign.free-inventory-full");
        }

        return SignUseResult.success(
                "service.sign.free-success",
                SignHandlerSupport.itemArguments(descriptor)
        );
    }

}
