package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.Map;

public final class TprCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportService teleports;
    private final RandomTeleportService randomTeleports;
    private final RandomTeleportSettingsService settings;
    private final int warmupSeconds;

    public TprCommand(
            PlatformService platform,
            TeleportService teleports,
            RandomTeleportService randomTeleports,
            RandomTeleportSettingsService settings,
            int warmupSeconds
    ) {
        this.platform = platform;
        this.teleports = teleports;
        this.randomTeleports = randomTeleports;
        this.settings = settings;
        this.warmupSeconds = Math.max(0, warmupSeconds);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.random";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "tpr";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.errorKey("commands.teleport.tpr-command.error.1");
            return 0;
        }
        var current = platform.location(self.orElseThrow());
        if (!invocation.hasPermission(worldPermission(current.world))) {
            invocation.errorKey("commands.teleport.world-no-permission", Map.of("world", current.world));
            return 0;
        }
        var location = randomTeleports.randomLocation(current.world, settings.settings(current.world));
        if (location.isEmpty()) {
            invocation.errorKey("commands.teleport.tpr-command.error.2");
            return 0;
        }
        invocation.replyKey("commands.teleport.tpr-command.reply.1");
        teleports.teleport(
                self.orElseThrow(),
                location.orElseThrow(),
                new TeleportOptions().warmupSeconds(warmupSeconds)
        ).thenAccept(result -> {
            if (result.success()) {
                invocation.replyKey(
                        "commands.teleport.tpr-command.success",
                        Map.of("location", result.location().compact())
                );
            } else {
                invocation.errorKey(result.message().key(), result.message().placeholders());
            }
        });
        return 1;
    }

    private String worldPermission(String world) {
        return "cellulosesz.teleport.world." + world.toLowerCase().replace(':', '.');
    }

}
