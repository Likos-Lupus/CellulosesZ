package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class SetJailCommand extends AbstractAdminCommand {

    private final JailService jails;

    public SetJailCommand(
            PlatformService platform,
            UserService users,
            JailService jails
    ) {
        super(platform, users);
        this.jails = jails;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.jail.set";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/setjail <name>";
    }

    @Override
    public String name() {
        return "setjail";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1) {
            invocation.errorKey(
                    "commands.admin.set-jail-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.errorKey("commands.admin.set-jail-command.error.2");
            return 0;
        }

        var result = jails.setJail(
                invocation.args()[0],
                platform.location(self.get()),
                actor(invocation)
        );
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

}
