package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

public final class ListCommand extends AbstractMessagingCommand {

    public ListCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config
    ) {
        super(platform, users, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.list";
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var names = platform.onlinePlayers().stream()
                .map(CellPlayer::name)
                .toList();
        invocation.reply("在线玩家(%d): %s".formatted(names.size(), String.join(", ", names)));
        return 1;
    }

}
