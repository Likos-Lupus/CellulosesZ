package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;

import java.util.Map;
import java.util.Optional;

public final class RestCommand implements CellCommand {

    private final PlatformService platform;
    private final PlayerStatePlatformService operations;

    public RestCommand(
            PlatformService platform,
            PlayerStatePlatformService operations
    ) {
        this.platform = platform;
        this.operations = operations;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.rest";
    }

    @Override
    public String usage() {
        return "/rest [player]";
    }

    @Override
    public String name() {
        return "rest";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) {
            invocation.errorKey("commands.playerstate.rest.usage", Map.of("usage", usage()));
            return 0;
        }
        var target = target(invocation);
        if (target.isEmpty()) return 0;
        var result = operations.resetRest(target.orElseThrow());
        if (!result.successful()) {
            invocation.errorKey("commands.playerstate.rest.platform-failed");
            return 0;
        }
        invocation.replyKey("commands.playerstate.rest.success", Map.of("player", target.orElseThrow().name()));
        return 1;
    }

    private Optional<CellPlayer> target(CommandInvocation invocation) {
        if (invocation.args().length == 0) {
            var self = platform.player(invocation);
            if (self.isEmpty()) invocation.errorKey("commands.playerstate.rest.console-target-required");
            return self;
        }
        if (!invocation.hasPermission("cellulosesz.command.rest.others")) {
            invocation.errorKey("commands.common.no-permission");
            return Optional.empty();
        }
        var target = invocation.resolvePlayer(invocation.args()[0]).online();
        if (target.isEmpty())
            invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[0]));
        return target;
    }

}
