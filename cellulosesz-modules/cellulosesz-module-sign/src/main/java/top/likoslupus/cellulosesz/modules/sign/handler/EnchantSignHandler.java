package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EnchantSignHandler implements SynchronousSignHandler {

    private final PlatformService platform;

    public EnchantSignHandler(PlatformService platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
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
        if (context.line(1).isBlank()) return Optional.empty();
        if (!context.line(2).isBlank()
                && SignHandlerSupport.count(context.line(2), 1, 255).isEmpty()) return Optional.empty();
        return Optional.of(new Parameters(
                context.line(1),
                SignHandlerSupport.count(context.line(2), 1, 255).orElse(1)
        ));
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        return parameters(context)
                .map(parameters -> platform.enchantHeldItem(context.player(), parameters.name(), parameters.level())
                        ? SignUseResult.success("service.sign.enchant-success", Map.of(
                        "enchantment", parameters.name(), "level", parameters.level()))
                        : SignUseResult.failure("service.sign.enchant-failed"))
                .orElseGet(() -> SignUseResult.failure("service.sign.enchant-format"));
    }

    private record Parameters(
            String name,
            int level
    ) {

    }

}
