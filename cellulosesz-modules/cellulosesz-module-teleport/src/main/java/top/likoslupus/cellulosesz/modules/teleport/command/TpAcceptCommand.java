package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

public final class TpAcceptCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportService teleports;
    private final TeleportRequestService requests;

    public TpAcceptCommand(
            PlatformService platform,
            TeleportService teleports,
            TeleportRequestService requests
    ) {
        this.platform = platform;
        this.teleports = teleports;
        this.requests = requests;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpaccept";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "tpaccept";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var target = platform.player(invocation);
        if (target.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-accept-command.error.1");
            return 0;
        }

        var request = requests.removeFor(target.get().uuid());
        if (request.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-accept-command.error.2");
            return 0;
        }

        var requester = platform.onlinePlayers().stream()
                .filter(player -> player.uuid().equals(request.get().requester()))
                .findFirst();
        if (requester.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-accept-command.error.3");
            return 0;
        }

        if (request.get().type() == TeleportRequestType.REQUESTER_TO_TARGET) {
            teleports.teleport(requester.get(), platform.location(target.get()), new TeleportOptions());
        } else {
            teleports.teleport(target.get(), platform.location(requester.get()), new TeleportOptions());
        }

        invocation.replyKey("commands.teleport.tp-accept-command.reply.1");
        return 1;
    }

}
