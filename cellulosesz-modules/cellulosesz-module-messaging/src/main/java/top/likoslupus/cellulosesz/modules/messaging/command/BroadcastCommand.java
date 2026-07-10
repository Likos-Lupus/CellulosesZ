package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.List;

public final class BroadcastCommand extends AbstractMessagingCommand {

    public BroadcastCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config
    ) {
        super(platform, users, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("broadcastworld");
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.broadcast";
    }

    @Override
    public String usage() {
        return "/broadcast <message>";
    }

    @Override
    public String name() {
        return "broadcast";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var message = join(args, 0);
        if (!validLength(invocation, message)) return 0;

        platform.onlinePlayers().forEach(player -> platform.sendMessage(player, "[公告] " + message));
        invocation.reply("公告已发送。 ");
        return 1;
    }

}
