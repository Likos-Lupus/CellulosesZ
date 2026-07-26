package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.teleport.service.TeleportRequestExecutor;

import java.util.Map;

public final class TpAcceptCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestExecutor executor;

    public TpAcceptCommand(PlatformService platform, TeleportRequestExecutor executor) {
        this.platform = platform;
        this.executor = executor;
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
    public String usage() {
        return "/tpaccept [request-id|player]";
    }

    @Override
    public String name() {
        return "tpaccept";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-accept-command.error.1");
            return 0;
        }
        if (invocation.args().length > 1) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", usage()));
            return 0;
        }
        if (invocation.args().length == 0) {
            return executor.accept(invocation, self.orElseThrow(), null, false) ? 1 : 0;
        }
        var token = invocation.args()[0];
        var requestId = parseUuid(token);
        if (requestId.isPresent()) {
            return executor.acceptRequest(
                    invocation, self.orElseThrow(), requestId.orElseThrow(), false
            ) ? 1 : 0;
        }
        var requester = invocation.resolvePlayer(token).optionalUuid();
        if (requester.isEmpty()) {
            invocation.errorKey("commands.teleport.request.unknown-player", Map.of("player", token));
            return 0;
        }
        return executor.accept(invocation, self.orElseThrow(), requester.orElseThrow(), false) ? 1 : 0;
    }

    private java.util.Optional<java.util.UUID> parseUuid(String value) {
        try {
            return java.util.Optional.of(java.util.UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }

}
