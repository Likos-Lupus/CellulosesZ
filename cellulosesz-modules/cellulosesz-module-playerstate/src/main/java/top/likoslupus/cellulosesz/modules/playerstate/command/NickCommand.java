package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Optional;

public final class NickCommand extends AbstractPlayerStateCommand {

    public NickCommand(
            PlatformService platform,
            UserService users,
            PlayerStateService states
    ) {
        super(platform, users, states);
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.nick";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/nick <name|off>";
    }

    @Override
    public String name() {
        return "nick";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = self(invocation);
        if (self.isEmpty()) return 0;

        if (invocation.args().length < 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var nick = invocation.args()[0].equalsIgnoreCase("off")
                || invocation.args()[0].equalsIgnoreCase("clear")
                ? Optional.<String>empty()
                : Optional.of(invocation.args()[0]);
        var result = states.setNick(
                self.get().uuid(),
                self.get().name(),
                nick
        );
        invocation.reply(result.message());
        return 1;
    }

}
