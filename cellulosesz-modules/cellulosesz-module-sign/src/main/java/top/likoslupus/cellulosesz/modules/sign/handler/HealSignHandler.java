package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Objects;

public final class HealSignHandler implements SynchronousSignHandler {

    private final PlayerStateService states;

    public HealSignHandler(PlayerStateService states) {
        this.states = Objects.requireNonNull(states, "states");
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
        return SignHandlerSupport.admin(states.heal(context.player()));
    }

}
