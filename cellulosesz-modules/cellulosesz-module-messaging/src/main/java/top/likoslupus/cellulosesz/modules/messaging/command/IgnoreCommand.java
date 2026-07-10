package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

public final class IgnoreCommand extends AbstractMessagingCommand {

    private final PrivateMessageService privateMessages;

    public IgnoreCommand(
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
        return "cellulosesz.messaging.ignore";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/ignore <player>";
    }

    @Override
    public String name() {
        return "ignore";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var self = player(invocation);
        var target = uuid(invocation, args[0]);
        if (self.isEmpty() || target.isEmpty()) return 0;

        var nowIgnored = !privateMessages.ignored(self.get().uuid(), target.get());
        privateMessages.setIgnored(self.get().uuid(), target.get(), nowIgnored);
        invocation.reply(nowIgnored ? "已忽略 " + args[0] + "。" : "已取消忽略 " + args[0] + "。 ");
        return 1;
    }

}
