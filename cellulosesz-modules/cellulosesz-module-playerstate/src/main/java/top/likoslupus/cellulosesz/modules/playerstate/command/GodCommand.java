package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

public final class GodCommand extends AbstractPlayerStateCommand {

    public GodCommand(
            PlatformService platform,
            UserService users,
            PlayerStateService states
    ) {
        super(platform, users, states);
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.god";
    }

    @Override
    public String usage() {
        return "/god [player] [on|off]";
    }

    @Override
    public String name() {
        return "god";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var target = target(invocation, 0, "cellulosesz.playerstate.god.other");
        if (target.isEmpty()) return 0;

        var current = users.cached(target.get().uuid())
                .map(user -> user.state.god)
                .orElse(false);
        var enabled = invocation.args().length > 1
                ? state(invocation.args()[1]).orElse(!current)
                : !current;
        states.setGod(target.get(), enabled).whenComplete((result, failure) -> {
            if (failure != null) invocation.errorKey("service.user.persistence-failed");
            else if (result.success()) invocation.reply(result.message());
            else invocation.error(result.message());
        });
        return 1;
    }

}
