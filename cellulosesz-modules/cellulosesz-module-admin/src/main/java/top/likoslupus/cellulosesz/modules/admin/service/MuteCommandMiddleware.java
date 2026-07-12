package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Set;

public final class MuteCommandMiddleware implements CommandMiddleware {

    private static final Set<String> BLOCKED = Set.of(
            "msg", "tell", "w", "r", "reply", "mail", "me", "helpop"
    );

    private final PlatformService platform;
    private final MuteService mutes;

    public MuteCommandMiddleware(
            PlatformService platform,
            MuteService mutes
    ) {
        this.platform = platform;
        this.mutes = mutes;
    }

    @Override
    public int invoke(
            CellCommand command,
            CommandInvocation invocation,
            CommandContinuation next
    ) {
        if (BLOCKED.contains(command.name().toLowerCase())) {
            var player = platform.player(invocation);
            if (player.isPresent() && mutes.muted(player.get().uuid())) {
                invocation.errorKey("commands.admin.mute-command-middleware.error.1");
                return 0;
            }
        }
        return next.proceed();
    }

}
