package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.Map;
import java.util.Optional;

abstract class AbstractTeleportCommand implements CellCommand {

    protected final PlatformService platform;
    protected final TeleportService teleports;

    AbstractTeleportCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        this.platform = platform;
        this.teleports = teleports;
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) {
            invocation.errorKey("commands.teleport.abstract-teleport-command.error.1");
        }
        return player;
    }

    protected Optional<CellPlayer> online(CommandInvocation invocation, String name) {
        var player = invocation.resolvePlayer(name).online();
        if (player.isEmpty()) {
            invocation.errorKey(
                    "commands.teleport.abstract-teleport-command.error.2",
                    Map.of("value0", name)
            );
        }
        return player;
    }

    protected int teleport(
            CommandInvocation invocation,
            CellPlayer player,
            CellLocation location
    ) {
        teleports.teleport(player, location, new TeleportOptions())
                .thenAccept(result -> {
                    if (result.success()) {
                        invocation.replyKey(
                                "commands.teleport.abstract-teleport-command.reply.1",
                                Map.of("value0", result.location().compact())
                        );
                    } else {
                        invocation.errorKey(
                                "commands.teleport.abstract-teleport-command.error.3",
                                Map.of("value0", result.message())
                        );
                    }
                });
        return 1;
    }

    protected Optional<Double> parseDouble(
            CommandInvocation invocation,
            String value,
            String name
    ) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            invocation.errorKey(
                    "commands.teleport.invalid-number",
                    Map.of(
                            "name", name,
                            "value", value
                    )
            );
            return Optional.empty();
        }
    }

    protected Optional<Integer> parseInt(
            CommandInvocation invocation,
            String value,
            String name
    ) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            invocation.errorKey(
                    "commands.teleport.invalid-integer",
                    Map.of(
                            "name", name,
                            "value", value
                    )
            );
            return Optional.empty();
        }
    }

}
