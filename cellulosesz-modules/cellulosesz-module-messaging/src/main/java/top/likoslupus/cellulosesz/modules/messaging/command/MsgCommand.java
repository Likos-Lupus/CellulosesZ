package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.List;
import java.util.Map;

public final class MsgCommand extends AbstractMessagingCommand {

    private final PrivateMessageService privateMessages;

    public MsgCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            PrivateMessageService privateMessages
    ) {
        super(platform, users, config);
        this.privateMessages = privateMessages;
    }

    @Override
    public List<String> aliases() {
        return List.of("tell", "w");
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.msg";
    }

    @Override
    public String usage() {
        return "/msg <player> <message>";
    }

    @Override
    public String name() {
        return "msg";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 2) {
            invocation.errorKey(
                    "commands.messaging.msg-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var sender = player(invocation);
        var target = online(invocation, args[0]);
        var message = join(args, 1);
        if (sender.isEmpty() || target.isEmpty() || !validLength(invocation, message)) return 0;

        var result = privateMessages.send(sender.get(), target.get(), message);
        if (!result.success()) invocation.error(result.message());
        return result.success() ? 1 : 0;
    }

}
