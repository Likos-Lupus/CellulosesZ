package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.item.ItemPlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class EnchantSignHandler implements SynchronousSignHandler {

    private final ItemPlatformService items;

    public EnchantSignHandler(ItemPlatformService items) {
        this.items = requireNonNull(items, "items");
    }

    @Override
    public String id() {
        return "Enchant";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return parameters(context).isPresent()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.enchant-format");
    }

    private Optional<Parameters> parameters(SignUseContext context) {
        var name = context.line(1).toLowerCase(Locale.ROOT);
        if (name.isBlank() || !items.enchantmentIds().contains(name)) {
            return Optional.empty();
        }

        if (!context.line(2).isBlank()
                && SignHandlerSupport.count(context.line(2), 1, 255).isEmpty()
        ) {
            return Optional.empty();
        }

        return Optional.of(new Parameters(
                name,
                SignHandlerSupport.count(context.line(2), 1, 255).orElse(1)
        ));
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        return parameters(context)
                .map(parameters -> items.enchant(
                                context.player(),
                                parameters.name(),
                                parameters.level(),
                                false
                        ).successful() ?
                                SignUseResult.success(
                                        "service.sign.enchant-success",
                                        Map.of(
                                                "enchantment", parameters.name(),
                                                "level", parameters.level()
                                        )
                                ) :
                                SignUseResult.failure("service.sign.enchant-failed")
                )
                .orElseGet(() -> SignUseResult.failure("service.sign.enchant-format"));
    }

    private record Parameters(
            String name,
            int level
    ) {

    }

}
