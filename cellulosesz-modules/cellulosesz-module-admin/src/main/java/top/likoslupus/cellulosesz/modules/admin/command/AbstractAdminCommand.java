package top.likoslupus.cellulosesz.modules.admin.command;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.service.DurationParser;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

abstract class AbstractAdminCommand implements CellCommand {

    protected final PlatformService platform;
    protected final UserService users;

    AbstractAdminCommand(
            PlatformService platform,
            UserService users
    ) {
        this.platform = platform;
        this.users = users;
    }

    protected String actor(CommandInvocation invocation) {
        return invocation.playerName().orElse("console");
    }

    protected String join(String[] args, int start) {
        if (start >= args.length) return "";
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    protected Optional<CellPlayer> online(CommandInvocation invocation, String name) {
        var player = platform.onlinePlayer(name);
        if (player.isEmpty()) invocation.error("找不到在线玩家: " + name);
        return player;
    }

    protected Optional<UUID> uuid(CommandInvocation invocation, String name) {
        var online = platform.onlinePlayer(name);
        if (online.isPresent()) return Optional.of(online.get().uuid());

        var cached = users.findUuidByName(name);
        if (cached.isEmpty()) invocation.error("找不到玩家: " + name);
        return cached;
    }

    protected @Nullable Long durationOrNull(String value) {
        return DurationParser.parseMillis(value).stream().boxed().findFirst().orElse(null);
    }

}
