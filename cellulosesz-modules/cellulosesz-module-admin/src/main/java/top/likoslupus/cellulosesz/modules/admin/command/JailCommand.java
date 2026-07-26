package top.likoslupus.cellulosesz.modules.admin.command;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.service.DurationParser;

import java.util.List;
import java.util.Map;

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
            invocation.errorKey(
                    "commands.admin.jail-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var target = online(invocation, args[0]);
        if (target.isEmpty()) return 0;

        if (args[1].equalsIgnoreCase("off")
                || args[1].equalsIgnoreCase("release")
        ) {
            jails.unjail(
                    target.get().uuid(),
                    target.get().name(),
                    actor(invocation)
            ).whenComplete((result, failure) -> complete(invocation, result, failure));
            return 1;
        }

        @Nullable Long duration = defaultDuration();
        var reasonStart = 2;
        if (args.length >= 3) {
            var parsed = DurationParser.parseMillis(args[2]);
            if (parsed.isPresent()) {
                duration = parsed.getAsLong();
                reasonStart = 3;
            }
        }

        if (duration != null
                && exceedsMaximum(duration)
                && !invocation.hasPermission("cellulosesz.admin.punishment.unlimited")
        ) {
            invocation.errorKey(
                    "commands.admin.maximum-punishment",
                    Map.of("seconds", config.maximumPunishmentSeconds)
            );
            return 0;
        }

        jails.jailPlayer(
                target.get(),
                args[1],
                actor(invocation),
                duration,
                join(args, reasonStart)
        ).whenComplete((result, failure) -> complete(invocation, result, failure));
        return 1;
    }

    private void complete(
            CommandInvocation invocation,
            AdminResult result,
            Throwable failure
    ) {
        if (failure != null) invocation.errorKey("service.admin.persistence-failed");
        else if (result.success()) invocation.reply(result.message());
        else invocation.error(result.message());
    }

    private @Nullable Long defaultDuration() {
        if (config.defaultJailSeconds <= 0) return null;
        try {
            return Math.multiplyExact(config.defaultJailSeconds, 1000L);
        } catch (ArithmeticException _) {
            return Long.MAX_VALUE;
        }
    }

    private boolean exceedsMaximum(long durationMillis) {
        if (config.maximumPunishmentSeconds < 0) return false;
        try {
            return durationMillis > Math.multiplyExact(config.maximumPunishmentSeconds, 1000L);
        } catch (ArithmeticException _) {
            return false;
        }
    }

}
