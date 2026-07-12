package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;

public final class TpDenyCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestService requests;

    public TpDenyCommand(
            PlatformService platform,
            TeleportRequestService requests
    ) {
        this.platform = platform;
        this.requests = requests;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpdeny";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "tpdeny";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-deny-command.error.1");
            return 0;
        }

        if (requests.removeFor(self.get().uuid()).isPresent()) {
            invocation.replyKey("commands.teleport.tp-deny-command.reply.1");
            return 1;
        }

        invocation.errorKey("commands.teleport.tp-deny-command.error.2");
        return 0;
    }

}
