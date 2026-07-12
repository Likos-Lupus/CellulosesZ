package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Arrays;
import java.util.Map;
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
        return invocation.playerName()
                .orElse("console");
    }

    protected String join(String[] args, int start) {
        if (start >= args.length) return "";
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    protected Optional<CellPlayer> online(CommandInvocation invocation, String name) {
        var player = invocation.resolvePlayer(name).online();
        if (player.isEmpty()) {
            invocation.errorKey(
                    "commands.admin.abstract-admin-command.error.1",
                    Map.of("value0", name)
            );
        }
        return player;
    }

    protected Optional<UUID> uuid(CommandInvocation invocation, String name) {
        var uuid = invocation.resolvePlayer(name).optionalUuid();
        if (uuid.isEmpty()) {
            invocation.errorKey(
                    "commands.admin.abstract-admin-command.error.2",
                    Map.of("value0", name)
            );
        }
        return uuid;
    }

}
