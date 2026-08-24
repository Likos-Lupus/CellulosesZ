package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

import static java.util.Objects.requireNonNull;

public final class BalanceSignHandler implements SynchronousSignHandler {

    private final EconomyService economy;

    public BalanceSignHandler(EconomyService economy) {
        this.economy = requireNonNull(economy, "economy");
    }

    @Override
    public String id() {
        return "Balance";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return SignHandlerSupport.noArguments(context, "service.sign.balance-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        return SignUseResult.success(
                "service.sign.balance",
                MessageArguments.builder()
                        .add(economy.format(economy.balance(context.player().uuid())))
                        .build()
        );
    }

}
