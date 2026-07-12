package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.Map;

public final class TpHereCommand extends AbstractTeleportCommand {

    public TpHereCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        super(platform, teleports);
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
                    "commands.teleport.tp-here-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var self = player(invocation);
        var target = online(invocation, args[0]);
        if (self.isEmpty() || target.isEmpty()) return 0;

        return teleport(invocation, target.get(), platform.location(self.get()));
    }

}
