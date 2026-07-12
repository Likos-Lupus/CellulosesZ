package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class JailedPlayersCommand extends AbstractAdminCommand {

    private final JailService jails;

    public JailedPlayersCommand(
            PlatformService platform,
            UserService users,
            JailService jails
    ) {
        super(platform, users);
        this.jails = jails;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.jail.list";
    }

    @Override
    public String name() {
        return "jailedplayers";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var names = jails.jailedPlayers().stream()
                .map(player -> "%s@%s".formatted(player.name, player.jail))
                .sorted()
                .toList();
        if (names.isEmpty()) {
            invocation.replyKey("commands.admin.jailed-players-empty");
        } else {
            invocation.replyKey(
                    "commands.admin.jailed-players",
                    Map.of("players", String.join(", ", names))
            );
        }
        return 1;
    }

}
