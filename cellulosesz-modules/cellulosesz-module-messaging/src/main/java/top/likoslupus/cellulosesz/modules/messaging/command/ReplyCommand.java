package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.List;

public final class ReplyCommand extends AbstractMessagingCommand {

    private final PrivateMessageService privateMessages;

    public ReplyCommand(
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
        return List.of("reply");
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.reply";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/r <message>";
    }

    @Override
    public String name() {
        return "r";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        var args = invocation.args();
        if (self.isEmpty()) return 0;

        if (args.length < 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var targetUuid = privateMessages.lastReplyTarget(self.get().uuid());
        if (targetUuid.isEmpty()) {
            invocation.error("没有可回复的玩家。 ");
            return 0;
        }

        var target = platform.onlinePlayers().stream()
                .filter(player -> player.uuid().equals(targetUuid.get()))
                .findFirst();
        if (target.isEmpty()) {
            invocation.error("可回复的玩家不在线。 ");
            return 0;
        }

        var message = join(args, 0);
        if (!validLength(invocation, message)) return 0;

        var result = privateMessages.send(self.get(), target.get(), message);
        if (!result.success()) invocation.error(result.message());
        return result.success() ? 1 : 0;
    }

}
