package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.service.DurationParser;

public final class MuteCommand extends AbstractAdminCommand {

    private final MuteService mutes;
    private final AdminConfig config;

    public MuteCommand(
            PlatformService platform,
            UserService users,
            MuteService mutes,
            AdminConfig config
    ) {
        super(platform, users);
        this.mutes = mutes;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.mute";
    }

    @Override
    public String usage() {
        return "/mute <player> [duration|off] [reason]";
    }

    @Override
    public String name() {
        return "mute";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var uuid = uuid(invocation, args[0]);
        if (uuid.isEmpty()) return 0;

        if (args.length >= 2
                && (args[1].equalsIgnoreCase("off")
                || args[1].equalsIgnoreCase("remove")
                || args[1].equalsIgnoreCase("clear"))
        ) {
            var result = mutes.unmute(uuid.get(), args[0], actor(invocation));
            if (result.success()) {
                invocation.reply(result.message());
            } else {
                invocation.error(result.message());
            }
            return result.success() ? 1 : 0;
        }

        var duration = config.defaultMuteSeconds <= 0
                ? null
                : config.defaultMuteSeconds * 1000L;
        var reasonStart = 1;
        if (args.length >= 2) {
            var parsed = DurationParser.parseMillis(args[1]);
            if (parsed.isPresent()) {
                duration = parsed.getAsLong();
                reasonStart = 2;
            }
        }

        var result = mutes.mute(
                uuid.get(),
                args[0],
                actor(invocation),
                duration,
                join(args, reasonStart)
        );
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

}
