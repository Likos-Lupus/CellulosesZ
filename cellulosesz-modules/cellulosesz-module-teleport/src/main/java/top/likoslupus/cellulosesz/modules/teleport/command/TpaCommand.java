package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.teleport.service.TeleportRequestExecutor;

import java.util.Map;

public final class TpaCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestExecutor executor;
    private final UserService users;
    private final int timeoutSeconds;
    private final boolean here;

    public TpaCommand(
            PlatformService platform,
            TeleportRequestExecutor executor,
            UserService users,
            int timeoutSeconds,
            boolean here
    ) {
        this.platform = platform;
        this.executor = executor;
        this.users = users;
        this.timeoutSeconds = timeoutSeconds;
        this.here = here;
    }

    @Override
    public String permission() {
        return here ? "cellulosesz.teleport.tpahere" : "cellulosesz.teleport.tpa";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/" + name() + " <player>";
    }

    @Override
    public String name() {
        return here ? "tpahere" : "tpa";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.errorKey(
                    "commands.teleport.tpa-command.error.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }
        var requester = platform.player(invocation);
        var target = invocation.resolvePlayer(args[0]).online();
        if (requester.isEmpty()) {
            invocation.errorKey("commands.teleport.tpa-command.error.command-can-only-used-by-player");
            return 0;
        }
        if (target.isEmpty()) {
            invocation.errorKey(
                    "commands.teleport.tpa-command.error.online-player-not-found",
                    Map.of("player", args[0])
            );
            return 0;
        }
        if (target.orElseThrow().uuid().equals(requester.orElseThrow().uuid())) {
            invocation.errorKey("commands.teleport.tpa-command.error.cannot-send-teleport-request-yourself");
            return 0;
        }
        users.load(target.orElseThrow().uuid()).whenComplete((targetUser, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.user.load-failed");
                return;
            }
            if (!targetUser.preferences.teleportRequests
                    && !invocation.hasPermission("cellulosesz.teleport.tptoggle.bypass")
            ) {
                invocation.errorKey(
                        "commands.teleport.tpa-command.requests-disabled",
                        Map.of("player", target.orElseThrow().name())
                );
                return;
            }
            platform.runOnServerThread(() -> {
                var creation = executor.create(
                        invocation,
                        requester.orElseThrow(),
                        target.orElseThrow(),
                        here
                                ? TeleportRequestType.TARGET_TO_REQUESTER
                                : TeleportRequestType.REQUESTER_TO_TARGET,
                        timeoutSeconds
                );
                if (!creation.created()) {
                    invocation.errorKey(
                            "commands.teleport.request.already-pending",
                            Map.of(
                                    "request", creation.request().id(),
                                    "player", target.orElseThrow().name()
                            )
                    );
                    return;
                }
                invocation.replyKey(
                        "commands.teleport.tpa-command.reply.sent-teleport-request-it-expires-seconds",
                        Map.of(
                                "player", target.orElseThrow().name(),
                                "expires_seconds", timeoutSeconds
                        )
                );
            });
        });
        return 1;
    }

}
