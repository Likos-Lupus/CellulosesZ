package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.Optional;

public final class NickCommand extends AbstractPlayerStateCommand {

    private final DisplayNameService displayNames;

    public NickCommand(
            PlatformService platform,
            UserService users,
            PlayerStateService states,
            DisplayNameService displayNames
    ) {
        super(platform, users, states);
        this.displayNames = displayNames;
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
            invocation.errorKey(
                    "commands.playerstate.nick-command.error.1",
                    Map.of("value0", usage())
            );
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
        if (!result.success()) {
            invocation.errorKey("player.nick-invalid");
            return 0;
        }

        var stored = states.nick(self.get().uuid());
        if (stored.isPresent()) {
            invocation.replyKey(
                    "player.nick-set",
                    Map.of("nickname", displayNames.displayName(self.get()).plainText())
            );
        } else {
            invocation.replyKey("player.nick-cleared");
        }
        return 1;
    }

}
