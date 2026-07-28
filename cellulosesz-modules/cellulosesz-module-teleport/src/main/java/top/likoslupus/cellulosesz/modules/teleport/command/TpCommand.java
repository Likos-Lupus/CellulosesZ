package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class TpCommand extends AbstractTeleportCommand {

    private final TeleportTargetPolicy policy;

    public TpCommand(
            PlatformService platform,
            TeleportService teleports,
            UserService users
    ) {
        super(platform, teleports);
        this.policy = new TeleportTargetPolicy(platform, users);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tp";
    }

    @Override
    public String usage() {
        return "/tp <target> | /tp <player> <target>";
    }

    @Override
    public String name() {
        return "tp";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length == 1) {
            var self = player(invocation);
            var target = online(invocation, args[0]);
            if (self.isEmpty() || target.isEmpty()) return 0;
            var subject = self.orElseThrow();
            var location = platform.location(target.orElseThrow());
            policy.mayMove(invocation, subject).thenAccept(allowed -> {
                if (allowed) teleport(invocation, subject, location);
            });
            return 1;
        }

        if (args.length == 2) {
            var subject = online(invocation, args[0]);
            var target = online(invocation, args[1]);
            if (subject.isEmpty() || target.isEmpty()) return 0;
            var moving = subject.orElseThrow();
            var location = platform.location(target.orElseThrow());
            policy.mayMove(invocation, moving).thenAccept(allowed -> {
                if (allowed) teleport(invocation, moving, location);
            });
            return 1;
        }

        invocation.errorKey(
                "commands.teleport.tp-command.error.usage",
                Map.of("usage", usage())
        );
        return 0;
    }

}
