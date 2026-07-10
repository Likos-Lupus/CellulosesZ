package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

public final class HealCommand extends AbstractPlayerStateCommand {

    public HealCommand(
            PlatformService platform,
            UserService users,
            PlayerStateService states
    ) {
        super(platform, users, states);
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.heal";
    }

    @Override
    public String usage() {
        return "/heal [player]";
    }

    @Override
    public String name() {
        return "heal";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var target = target(
                invocation,
                0,
                "cellulosesz.playerstate.heal.other"
        );
        if (target.isEmpty()) return 0;

        var result = states.heal(target.get());
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

}
