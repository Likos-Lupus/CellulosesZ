package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class TpaCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestService requests;
    private final UserService users;
    private final int timeoutSeconds;
    private final boolean here;

    public TpaCommand(
            PlatformService platform,
            TeleportRequestService requests,
            UserService users,
            int timeoutSeconds,
            boolean here
    ) {
        this.platform = platform;
        this.requests = requests;
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
                    "commands.teleport.tpa-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var requester = platform.player(invocation);
        var target = invocation.resolvePlayer(args[0]).online();

        if (requester.isEmpty()) {
            invocation.errorKey("commands.teleport.tpa-command.error.2");
            return 0;
        }

        if (target.isEmpty()) {
            invocation.errorKey(
                    "commands.teleport.tpa-command.error.3",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        if (target.get().uuid().equals(requester.get().uuid())) {
            invocation.errorKey("commands.teleport.tpa-command.error.4");
            return 0;
        }
        if (!users.load(target.get().uuid()).join().preferences.teleportRequests
                && !invocation.hasPermission("cellulosesz.teleport.tptoggle.bypass")
        ) {
            invocation.errorKey(
                    "commands.teleport.tpa-command.requests-disabled",
                    Map.of("player", target.get().name())
            );
            return 0;
        }

        requests.create(
                requester.get(),
                target.get(),
                here
                        ? TeleportRequestType.TARGET_TO_REQUESTER
                        : TeleportRequestType.REQUESTER_TO_TARGET,
                timeoutSeconds
        );
        invocation.replyKey(
                "commands.teleport.tpa-command.reply.1",
                Map.of(
                        "value0", target.get().name(),
                        "value1", timeoutSeconds
                )
        );
        return 1;
    }

}
