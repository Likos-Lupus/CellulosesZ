package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

public final class BottomCommand extends AbstractTeleportCommand {

    public BottomCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.bottom";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "bottom";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var current = platform.location(self.get());
        var target = current.withPosition(current.x, 0.0D, current.z);
        var safe = platform.safeLocation(target);

        if (safe.isEmpty()) {
            invocation.errorKey("commands.teleport.bottom-command.error.1");
            return 0;
        }

        return teleport(invocation, self.get(), safe.get());
    }

}
