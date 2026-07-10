package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.service.DurationParser;

import java.util.List;

public final class JailCommand extends AbstractAdminCommand {

    private final JailService jails;
    private final AdminConfig config;

    public JailCommand(
            PlatformService platform,
            UserService users,
            JailService jails,
            AdminConfig config
    ) {
        super(platform, users);
        this.jails = jails;
        this.config = config;
    }

    @Override
    public List<String> aliases() {
        return List.of("togglejail");
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.jail";
    }

    @Override
    public String usage() {
        return "/jail <player> <jail|off> [duration] [reason]";
    }

    @Override
    public String name() {
        return "jail";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 2) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var target = online(invocation, args[0]);
        if (target.isEmpty()) return 0;

        if (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("release")) {
            var result = jails.unjail(
                    target.get().uuid(),
                    target.get().name(),
                    actor(invocation)
            );
            if (result.success()) {
                invocation.reply(result.message());
            } else {
                invocation.error(result.message());
            }
            return result.success() ? 1 : 0;
        }

        var duration = config.defaultJailSeconds <= 0
                ? null
                : config.defaultJailSeconds * 1000L;
        var reasonStart = 2;
        if (args.length >= 3) {
            var parsed = DurationParser.parseMillis(args[2]);
            if (parsed.isPresent()) {
                duration = parsed.getAsLong();
                reasonStart = 3;
            }
        }

        var result = jails.jailPlayer(
                target.get(),
                args[1],
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
