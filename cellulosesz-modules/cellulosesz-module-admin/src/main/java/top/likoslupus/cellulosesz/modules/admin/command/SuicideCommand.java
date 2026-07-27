package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.KillKind;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;

import java.util.Map;

public final class SuicideCommand implements CellCommand {

    private final PlatformService platform;
    private final PlayerStatePlatformService players;

    public SuicideCommand(
            PlatformService platform,
            PlayerStatePlatformService players
    ) {
        this.platform = platform;
        this.players = players;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.suicide";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "suicide";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 0) {
            invocation.errorKey("commands.admin.suicide.usage", Map.of("usage", usage()));
            return 0;
        }
        var player = platform.player(invocation).orElseThrow();
        var result = players.kill(player, KillKind.SUICIDE, false);
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.admin.suicide.goodbye");
        invocation.replyKey("commands.admin.suicide.success", Map.of("player", player.name()));
        return 1;
    }

}
