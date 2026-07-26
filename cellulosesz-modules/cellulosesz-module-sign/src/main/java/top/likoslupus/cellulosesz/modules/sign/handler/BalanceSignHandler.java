package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Map;
import java.util.Objects;

public final class BalanceSignHandler implements SynchronousSignHandler {

    private final EconomyService economy;

    public BalanceSignHandler(EconomyService economy) {
        this.economy = Objects.requireNonNull(economy, "economy");
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
                Map.of("balance", economy.format(economy.balance(context.player().uuid())))
        );
    }

}
