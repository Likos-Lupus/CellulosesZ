package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.ItemPlatformService;
import top.likoslupus.cellulosesz.api.item.RepairScope;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public final class RepairSignHandler implements SynchronousSignHandler {

    private static final Set<String> MODES = Set.of("", "hand", "all");

    private final ItemPlatformService items;

    public RepairSignHandler(ItemPlatformService items) {
        this.items = requireNonNull(items, "items");
    }

    @Override
    public String id() {
        return "Repair";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return MODES.contains(context.line(1).toLowerCase(Locale.ROOT))
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.repair-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var scope = context.line(1).equalsIgnoreCase("all")
                ? RepairScope.ALL
                : RepairScope.HAND;
        var result = items.repair(context.player(), scope);
        var count = result.value().orElse(0);

        return result.successful() && count > 0
                ?
                SignUseResult.success(
                        "service.sign.repair-success",
                        Map.of("count", count)
                )
                : SignUseResult.failure("service.sign.repair-nothing");
    }

}
