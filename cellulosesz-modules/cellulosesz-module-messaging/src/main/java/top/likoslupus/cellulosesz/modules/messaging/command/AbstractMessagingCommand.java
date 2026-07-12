package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Arrays;
import java.util.Map;
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
        if (player.isEmpty()) {
            invocation.errorKey("commands.messaging.abstract-messaging-command.error.1");
        }
        return player;
    }

    protected Optional<CellPlayer> online(CommandInvocation invocation, String name) {
        var player = invocation.resolvePlayer(name).online();
        if (player.isEmpty()) {
            invocation.errorKey(
                    "commands.messaging.abstract-messaging-command.error.2",
                    Map.of("value0", name)
            );
        }
        return player;
    }

    protected Optional<UUID> uuid(CommandInvocation invocation, String name) {
        var uuid = invocation.resolvePlayer(name).optionalUuid();
        if (uuid.isEmpty()) {
            invocation.errorKey(
                    "commands.messaging.abstract-messaging-command.error.3",
                    Map.of("value0", name)
            );
        }
        return uuid;
    }

    protected String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    protected boolean validLength(CommandInvocation invocation, String message) {
        if (message.isBlank()) {
            invocation.errorKey("commands.messaging.abstract-messaging-command.error.4");
            return false;
        }
        if (message.length() > config.maxMessageLength) {
            invocation.errorKey(
                    "commands.messaging.abstract-messaging-command.error.5",
                    Map.of("value0", config.maxMessageLength)
            );
            return false;
        }
        return true;
    }

}
