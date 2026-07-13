package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;

import java.util.Locale;

public final class MuteCommandMiddleware implements CommandMiddleware {

    private final PlatformService platform;
    private final MuteService mutes;
    private final AdminConfig config;

    public MuteCommandMiddleware(
            PlatformService platform,
            MuteService mutes,
            AdminConfig config
    ) {
        this.platform = platform;
        this.mutes = mutes;
        this.config = config;
    }

    @Override
    public int invoke(
            CellCommand command,
            CommandInvocation invocation,
            CommandContinuation next
    ) {
        if (blocked(command.name())
                && !invocation.hasPermission("cellulosesz.admin.mute.bypass")
        ) {
            var player = platform.player(invocation);
            if (player.isPresent() && mutes.muted(player.get().uuid())) {
                invocation.errorKey("commands.admin.mute-command-middleware.error.1");
                return 0;
            }
        }
        return next.proceed();
    }

    public boolean blocked(String root) {
        var normalized = root.trim().toLowerCase(Locale.ROOT);
        return config.muteCommands.stream().anyMatch(value ->
                value.equals("*") || value.equalsIgnoreCase(normalized)
        );
    }

}
