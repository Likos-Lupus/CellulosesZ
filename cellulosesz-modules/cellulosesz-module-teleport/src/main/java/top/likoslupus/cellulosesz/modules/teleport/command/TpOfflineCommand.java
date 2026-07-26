package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.OfflineLocationService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.Map;

public final class TpOfflineCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportService teleports;
    private final OfflineLocationService offlineLocations;

    public TpOfflineCommand(
            PlatformService platform,
            TeleportService teleports,
            OfflineLocationService offlineLocations
    ) {
        this.platform = platform;
        this.teleports = teleports;
        this.offlineLocations = offlineLocations;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpoffline";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/tpoffline <player>";
    }

    @Override
    public String name() {
        return "tpoffline";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 1) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", usage()));
            return 0;
        }
        var self = platform.player(invocation);
        var target = invocation.resolvePlayer(invocation.args()[0]);
        if (self.isEmpty() || target.optionalUuid().isEmpty()) {
            invocation.errorKey("commands.teleport.request.unknown-player", Map.of("player", invocation.args()[0]));
            return 0;
        }
        if (target.online().isPresent()) {
            invocation.errorKey("commands.teleport.tpoffline.online", Map.of("player", target.name()));
            return 0;
        }
        var location = offlineLocations.location(target.optionalUuid().orElseThrow());
        if (location.isEmpty()) {
            invocation.errorKey("commands.teleport.tpoffline.no-location", Map.of("player", target.name()));
            return 0;
        }
        teleports.teleport(self.orElseThrow(), location.orElseThrow(), new TeleportOptions())
                .thenAccept(result -> {
                    if (result.success()) {
                        invocation.replyKey("commands.teleport.tpoffline.success", Map.of("player", target.name()));
                    } else {
                        invocation.errorKey(result.message().key(), result.message().placeholders());
                    }
                });
        return 1;
    }

}
