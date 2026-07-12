package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class DelJailCommand extends AbstractAdminCommand {

    private final JailService jails;

    public DelJailCommand(
            PlatformService platform,
            UserService users,
            JailService jails
    ) {
        super(platform, users);
        this.jails = jails;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.jail.delete";
    }

    @Override
    public String usage() {
        return "/deljail <name>";
    }

    @Override
    public String name() {
        return "deljail";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1) {
            invocation.errorKey(
                    "commands.admin.del-jail-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var result = jails.deleteJail(invocation.args()[0]);
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

}
