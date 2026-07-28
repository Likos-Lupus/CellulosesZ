package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

abstract class AbstractWarpCommand implements CellCommand {

    protected final PlatformService platform;
    protected final WarpService warps;
    protected final TeleportService teleports;
    protected final WarpConfig config;

    AbstractWarpCommand(
            PlatformService platform,
            WarpService warps,
            TeleportService teleports,
            WarpConfig config
    ) {
        this.platform = platform;
        this.warps = warps;
        this.teleports = teleports;
        this.config = config;
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty())
            invocation.errorKey("commands.warp.abstract-warp-command.error.command-can-only-used-by-player");
        return player;
    }

    protected boolean validName(CommandInvocation invocation, String name) {
        if (name.isBlank() || name.length() > config.naming.maxLength) {
            invocation.errorKey(
                    "commands.warp.abstract-warp-command.error.warp-names-cannot-empty-longer-than-characters",
                    Map.of("maximum_length", config.naming.maxLength)
            );
            return false;
        }
        if (!Pattern.matches(config.naming.pattern, name)) {
            invocation.errorKey("commands.warp.abstract-warp-command.error.warp-names-may-only-contain-configured-characters");
            return false;
        }
        return true;
    }

    protected TeleportOptions options(CommandInvocation invocation) {
        var warmup = invocation.hasPermission("cellulosesz.warp.bypass-warmup")
                ? 0
                : config.teleport.warmupSeconds;
        return new TeleportOptions()
                .safe(config.teleport.safe)
                .warmupSeconds(warmup);
    }

}
