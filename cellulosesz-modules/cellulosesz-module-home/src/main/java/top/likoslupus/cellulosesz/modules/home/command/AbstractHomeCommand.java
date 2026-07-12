package top.likoslupus.cellulosesz.modules.home.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

abstract class AbstractHomeCommand implements CellCommand {

    protected final PlatformService platform;
    protected final HomeService homes;
    protected final TeleportService teleports;
    protected final HomeConfig config;

    AbstractHomeCommand(
            PlatformService platform,
            HomeService homes,
            TeleportService teleports,
            HomeConfig config
    ) {
        this.platform = platform;
        this.homes = homes;
        this.teleports = teleports;
        this.config = config;
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) invocation.errorKey("commands.home.abstract-home-command.error.1");
        return player;
    }

    protected String nameOrDefault(String[] args) {
        return args.length == 0 ? "home" : args[0];
    }

    protected boolean validName(CommandInvocation invocation, String name) {
        if (name.length() < config.naming.minLength || name.length() > config.naming.maxLength) {
            invocation.errorKey(
                    "commands.home.abstract-home-command.error.2",
                    Map.of(
                            "value0", config.naming.minLength,
                            "value1", config.naming.maxLength
                    )
            );
            return false;
        }
        if (!Pattern.matches(config.naming.pattern, name)) {
            invocation.errorKey("commands.home.abstract-home-command.error.3");
            return false;
        }
        return true;
    }

    protected TeleportOptions options(CommandInvocation invocation) {
        var warmup = invocation.hasPermission("cellulosesz.home.bypass-warmup")
                ? 0
                : config.teleport.warmupSeconds;
        return new TeleportOptions()
                .safe(config.teleport.safe)
                .warmupSeconds(warmup);
    }

}
