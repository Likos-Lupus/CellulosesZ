package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.Map;

public final class TpoHereCommand extends AbstractTeleportCommand {

    public TpoHereCommand(PlatformService platform, TeleportService teleports) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpohere";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/tpohere <player>";
    }

    @Override
    public String name() {
        return "tpohere";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 1) {
            invocation.errorKey("commands.teleport.tp-here-command.error.1", Map.of("value0", usage()));
            return 0;
        }
        var self = player(invocation);
        var target = online(invocation, invocation.args()[0]);
        if (self.isEmpty() || target.isEmpty()) return 0;
        return teleport(invocation, target.orElseThrow(), platform.location(self.orElseThrow()));
    }

}
