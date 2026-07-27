package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;

import java.util.Map;
import java.util.Optional;

public final class ExtCommand implements CellCommand {

    private final PlatformService platform;
    private final PlayerStatePlatformService players;

    public ExtCommand(
            PlatformService platform,
            PlayerStatePlatformService players
    ) {
        this.platform = platform;
        this.players = players;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.ext";
    }

    @Override
    public String usage() {
        return "/ext [player]";
    }

    @Override
    public String name() {
        return "ext";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        var target = target(invocation);
        if (target.isEmpty()) return 0;
        var result = players.extinguish(target.orElseThrow());
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.admin.ext.success", Map.of("player", target.orElseThrow().name()));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.admin.ext.usage", Map.of("usage", usage()));
        return 0;
    }

    private Optional<CellPlayer> target(CommandInvocation invocation) {
        if (invocation.args().length == 0) {
            var self = platform.player(invocation);
            if (self.isEmpty()) invocation.errorKey("commands.admin.ext.console-target-required");
            return self;
        }
        if (!invocation.hasPermission("cellulosesz.command.ext.others")) {
            invocation.errorKey("commands.common.no-permission");
            return Optional.empty();
        }
        var target = invocation.resolvePlayer(invocation.args()[0]).online();
        if (target.isEmpty())
            invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[0]));
        return target;
    }

}
