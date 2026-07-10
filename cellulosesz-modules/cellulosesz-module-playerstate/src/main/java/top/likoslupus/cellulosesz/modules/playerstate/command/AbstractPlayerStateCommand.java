package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Optional;

abstract class AbstractPlayerStateCommand implements CellCommand {

    protected final PlatformService platform;
    protected final UserService users;
    protected final PlayerStateService states;

    AbstractPlayerStateCommand(
            PlatformService platform,
            UserService users,
            PlayerStateService states
    ) {
        this.platform = platform;
        this.users = users;
        this.states = states;
    }

    protected Optional<CellPlayer> target(
            CommandInvocation invocation,
            int index,
            String otherPermission
    ) {
        if (invocation.args().length > index) {
            if (!invocation.hasPermission(otherPermission)) {
                invocation.error("你没有权限操作其他玩家。");
                return Optional.empty();
            }

            var player = platform.onlinePlayer(invocation.args()[index]);
            if (player.isEmpty()) invocation.error("找不到在线玩家: " + invocation.args()[index]);
            return player;
        }
        return self(invocation);
    }

    protected Optional<CellPlayer> self(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) invocation.error("此命令只能由玩家执行。");
        return player;
    }

    protected Optional<Boolean> state(String value) {
        if (value.equalsIgnoreCase("on")
                || value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("enable")
        ) return Optional.of(true);

        if (value.equalsIgnoreCase("off")
                || value.equalsIgnoreCase("false")
                || value.equalsIgnoreCase("disable")
        ) return Optional.of(false);

        return Optional.empty();
    }

}
