package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

public final class AfkCommand extends AbstractPlayerStateCommand {

    public AfkCommand(
            PlatformService platform,
            UserService users,
            PlayerStateService states
    ) {
        super(platform, users, states);
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.afk";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "afk";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = self(invocation);
        if (self.isEmpty()) return 0;

        var enabled = !states.afk(self.get().uuid());
        var result = states.setAfk(
                self.get().uuid(),
                self.get().name(),
                enabled
        );
        invocation.reply(result.message());
        return 1;
    }

}
