package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.TempBanService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.service.DurationParser;

import java.util.Map;

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
            invocation.errorKey(
                    "commands.admin.temp-ban-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var duration = DurationParser.parseMillis(args[1]);
        if (duration.isEmpty()) {
            invocation.errorKey("commands.admin.temp-ban-command.error.2");
            return 0;
        }

        var target = invocation.resolvePlayer(args[0]);
        if (target.optionalUuid().isEmpty()) {
            invocation.errorKey(
                    "commands.admin.abstract-admin-command.error.2",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        var result = bans.tempBan(
                target.name(),
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
