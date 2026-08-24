package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

import static java.util.Objects.requireNonNull;

public final class HealSignHandler implements SynchronousSignHandler {

    private final PlayerStateService states;

    public HealSignHandler(PlayerStateService states) {
        this.states = requireNonNull(states, "states");
    }

    @Override
    public String id() {
        return "Heal";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return SignHandlerSupport.noArguments(context, "service.sign.heal-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        return SignHandlerSupport.outcome(states.heal(context.player()));
    }

}
