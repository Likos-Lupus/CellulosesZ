package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

abstract class AbstractMessagingCommand implements CellCommand {

    protected final PlatformService platform;
    protected final UserService users;
    protected final MessagingConfig config;

    AbstractMessagingCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config
    ) {
        this.platform = platform;
        this.users = users;
        this.config = config;
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) invocation.error("此命令只能由玩家执行。");
        return player;
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

    protected String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    protected boolean validLength(CommandInvocation invocation, String message) {
        if (message.isBlank()) {
            invocation.error("消息不能为空。 ");
            return false;
        }
        if (message.length() > config.maxMessageLength) {
            invocation.error("消息太长，最大长度为 " + config.maxMessageLength + "。 ");
            return false;
        }
        return true;
    }

}
