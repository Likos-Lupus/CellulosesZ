package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class KickCommand extends AbstractAdminCommand {

    private final BanService bans;

    public KickCommand(
            PlatformService platform,
            UserService users,
            BanService bans
    ) {
        super(platform, users);
        this.bans = bans;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.kick";
    }

    @Override
    public String usage() {
        return "/kick <player> [reason]";
    }

    @Override
    public String name() {
        return "kick";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1) {
            invocation.errorKey(
                    "commands.admin.kick-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var target = invocation.resolvePlayer(invocation.args()[0]).online();
        if (target.isEmpty()) {
            invocation.errorKey(
                    "commands.admin.abstract-admin-command.error.1",
                    Map.of("value0", invocation.args()[0])
            );
            return 0;
        }

        var result = bans.kick(
                target.get().name(),
                actor(invocation),
                join(invocation.args(), 1)
        );
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

}
