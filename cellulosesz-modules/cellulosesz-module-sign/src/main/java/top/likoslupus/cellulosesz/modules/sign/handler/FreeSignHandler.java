package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FreeSignHandler implements SynchronousSignHandler {

    private final ItemService items;
    private final PlatformService platform;

    public FreeSignHandler(ItemService items, PlatformService platform) {
        this.items = Objects.requireNonNull(items, "items");
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public String id() {
        return "Free";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        var item = item(context);
        var result = SignHandlerSupport.validateItem(items, item, "service.sign.free-format");
        if (!result.success()) return result;
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
        var count = SignHandlerSupport.count(context.line(1), 1, 64);
        if (count.isEmpty() || context.line(2).isBlank()) return Optional.empty();
        return items.parse(context.line(2) + " " + count.orElseThrow());
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var item = item(context);
        if (item.isEmpty()) return SignUseResult.failure("service.sign.free-format");
        var request = new InventoryItemRequest(items.commandArgument(item.orElseThrow()), item.orElseThrow().count);
        var mutation = platform.prepareInventoryExchange(context.player(), List.of(), List.of(request));
        if (mutation.isEmpty() || !mutation.orElseThrow().commit()) {
            return SignUseResult.failure("service.sign.free-inventory-full");
        }
        return SignUseResult.success("service.sign.free-success", SignHandlerSupport.itemPlaceholders(item.orElseThrow()));
    }

}
