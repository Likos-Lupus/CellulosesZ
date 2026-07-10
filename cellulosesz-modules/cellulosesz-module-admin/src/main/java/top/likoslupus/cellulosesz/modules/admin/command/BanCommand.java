package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

public final class BanCommand extends AbstractAdminCommand {

    private final BanService bans;

    public BanCommand(
            PlatformService platform,
            UserService users,
            BanService bans
    ) {
        super(platform, users);
        this.bans = bans;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.ban";
    }

    @Override
    public String usage() {
        return "/ban <player> [reason]";
    }

    @Override
    public String name() {
        return "ban";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var result = bans.ban(
                invocation.args()[0],
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
