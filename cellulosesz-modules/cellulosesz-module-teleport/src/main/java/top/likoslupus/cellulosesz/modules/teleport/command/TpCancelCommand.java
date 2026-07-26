package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;

import java.util.Map;

public final class TpCancelCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestService requests;

    public TpCancelCommand(PlatformService platform, TeleportRequestService requests) {
        this.platform = platform;
        this.requests = requests;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpacancel";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/tpacancel [request-id|player]";
    }

    @Override
    public String name() {
        return "tpacancel";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-cancel-command.error.1");
            return 0;
        }
        if (invocation.args().length > 1) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", usage()));
            return 0;
        }
        int removed;
        if (invocation.args().length == 0) {
            removed = requests.cancel(self.orElseThrow().uuid(), null);
        } else {
            var token = invocation.args()[0];
            var request = requestById(token);
            if (request.isPresent()) {
                var value = request.orElseThrow();
                removed = value.requester().equals(self.orElseThrow().uuid()) && requests.remove(value.id())
                        ? 1
                        : 0;
            } else {
                var target = invocation.resolvePlayer(token).optionalUuid();
                if (target.isEmpty()) {
                    invocation.errorKey("commands.teleport.request.unknown-player", Map.of("player", token));
                    return 0;
                }
                removed = requests.cancel(self.orElseThrow().uuid(), target.orElseThrow());
            }
        }
        if (removed <= 0) {
            invocation.errorKey("commands.teleport.tp-cancel-command.error.2");
            return 0;
        }
        invocation.replyKey("commands.teleport.tp-cancel-command.reply.1", Map.of("count", removed));
        return removed;
    }

    private java.util.Optional<top.likoslupus.cellulosesz.api.teleport.TeleportRequest> requestById(String value) {
        try {
            return requests.pending(java.util.UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }

}
