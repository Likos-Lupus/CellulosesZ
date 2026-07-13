package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;


public final class MsgToggleCommand extends AbstractMessagingCommand {

    public MsgToggleCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config
    ) {
        super(platform, users, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.toggle";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "msgtoggle";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var user = users.load(self.get().uuid()).join();
        user.preferences.privateMessages = !user.preferences.privateMessages;
        users.markDirty(self.get().uuid());
        users.save(self.get().uuid());
        invocation.replyKey(
                user.preferences.privateMessages
                        ? "commands.messaging.private-messages-enabled"
                        : "commands.messaging.private-messages-disabled"
        );
        return 1;
    }

}
