package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.TempBanService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.service.DurationParser;

public final class TempBanCommand extends AbstractAdminCommand {

    private final TempBanService bans;

    public TempBanCommand(
            PlatformService platform,
            UserService users,
            TempBanService bans
    ) {
        super(platform, users);
        this.bans = bans;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.tempban";
    }

    @Override
    public String usage() {
        return "/tempban <player> <duration> [reason]";
    }

    @Override
    public String name() {
        return "tempban";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 2) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var duration = DurationParser.parseMillis(args[1]);
        if (duration.isEmpty()) {
            invocation.error("时间格式错误，例如 10m、2h、7d。");
            return 0;
        }

        var result = bans.tempBan(
                args[0],
                actor(invocation),
                duration.getAsLong(),
                join(args, 2)
        );
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

}
