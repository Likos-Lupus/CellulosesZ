package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class TpHereCommand extends AbstractTeleportCommand {

    private final TeleportTargetPolicy policy;

    public TpHereCommand(
            PlatformService platform,
            TeleportService teleports,
            UserService users
    ) {
        super(platform, teleports);
        this.policy = new TeleportTargetPolicy(platform, users);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tphere";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/tphere <player>";
    }

    @Override
    public String name() {
        return "tphere";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.errorKey(
                    "commands.teleport.tp-here-command.error.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var self = player(invocation);
        var target = online(invocation, args[0]);
        if (self.isEmpty() || target.isEmpty()) return 0;
        var moving = target.orElseThrow();
        var location = platform.location(self.orElseThrow());
        policy.mayMove(invocation, moving).thenAccept(allowed -> {
            if (allowed) teleport(invocation, moving, location);
        });
        return 1;
    }

}
