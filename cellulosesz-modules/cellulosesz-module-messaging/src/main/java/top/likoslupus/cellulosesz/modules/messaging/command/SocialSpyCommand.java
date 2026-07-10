package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

public final class SocialSpyCommand extends AbstractMessagingCommand {

    private final PrivateMessageService privateMessages;

    public SocialSpyCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            PrivateMessageService privateMessages
    ) {
        super(platform, users, config);
        this.privateMessages = privateMessages;
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.socialspy";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "socialspy";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var enabled = !privateMessages.socialSpy(self.get().uuid());
        privateMessages.setSocialSpy(self.get().uuid(), enabled);
        invocation.reply(enabled ? "已开启 SocialSpy。" : "已关闭 SocialSpy。 ");
        return 1;
    }

}
