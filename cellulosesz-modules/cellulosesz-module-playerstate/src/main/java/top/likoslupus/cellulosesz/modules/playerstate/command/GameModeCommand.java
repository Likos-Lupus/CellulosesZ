package top.likoslupus.cellulosesz.modules.playerstate.command;

import org.jspecify.annotations.Nullable;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;
import java.util.Optional;

public final class GameModeCommand implements CellCommand {

    private final PlatformService platform;

    public GameModeCommand(PlatformService platform) {
        this.platform = platform;
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.gamemode";
    }

    @Override
    public String usage() {
        return "/gamemode <survival|creative|adventure|spectator> [player]";
    }

    @Override
    public String name() {
        return "gamemode";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();

        if (args.length < 1 || args.length > 2) {
            invocation.errorKey(
                    "commands.playerstate.gamemode-usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var target = target(invocation, args.length == 2 ? args[1] : null);
        if (target.isEmpty()) return 0;

        if (args.length == 2
                && !invocation.hasPermission("cellulosesz.playerstate.gamemode.others")
        ) {
            invocation.errorKey("common.no-permission");
            return 0;
        }

        if (!platform.setGameMode(target.get(), args[0])) {
            invocation.errorKey(
                    "commands.playerstate.gamemode-invalid",
                    Map.of("mode", args[0])
            );
            return 0;
        }

        invocation.replyKey(
                "commands.playerstate.gamemode-set",
                Map.of(
                        "player", target.get().name(),
                        "mode", platform.gameMode(target.get()).orElse(args[0])
                )
        );
        return 1;
    }

    private Optional<CellPlayer> target(CommandInvocation invocation, @Nullable String name) {
        if (name == null) {
            var self = platform.player(invocation);
            if (self.isEmpty()) {
                invocation.errorKey("commands.common.player-required");
            }
            return self;
        }

        var target = invocation.resolvePlayer(name).online();
        if (target.isEmpty()) {
            invocation.errorKey(
                    "commands.common.player-offline",
                    Map.of("player", name)
            );
        }
        return target;
    }

}
