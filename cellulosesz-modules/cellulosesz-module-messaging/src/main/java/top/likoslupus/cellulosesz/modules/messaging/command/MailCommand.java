package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.messaging.MailMessage;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.UUID;

public final class MailCommand extends AbstractMessagingCommand {

    private final MailService mail;

    public MailCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            MailService mail
    ) {
        super(platform, users, config);
        this.mail = mail;
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.mail";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/mail <read|send|clear>";
    }

    @Override
    public String name() {
        return "mail";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length == 0 || args[0].equalsIgnoreCase("read")) {
            mail.inbox(self.get().uuid()).thenAccept(messages -> {
                if (messages.isEmpty()) {
                    invocation.reply("你没有邮件。 ");
                } else {
                    var builder = new StringBuilder("邮件:");
                    messages.forEach(message -> builder.append("\n- ")
                            .append(message.fromName)
                            .append(": ")
                            .append(message.message));
                    invocation.reply(builder.toString());
                    mail.markRead(self.get().uuid());
                }
            });
            return 1;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            mail.clear(self.get().uuid()).thenRun(() -> invocation.reply("已清空邮件。"));
            return 1;
        }

        if (args[0].equalsIgnoreCase("send")) {
            if (args.length < 3) {
                invocation.error("用法: /mail send <player> <message>");
                return 0;
            }

            var recipient = uuid(invocation, args[1]);
            var messageText = join(args, 2);
            if (recipient.isEmpty() || !validLength(invocation, messageText)) return 0;

            var message = new MailMessage();
            message.id = UUID.randomUUID().toString();
            message.fromUuid = self.get().uuid();
            message.fromName = self.get().name();
            message.toUuid = recipient.get();
            message.message = messageText;
            mail.send(recipient.get(), message).thenRun(() -> invocation.reply("邮件已发送给 " + args[1] + "。"));
            platform.onlinePlayer(args[1])
                    .ifPresent(player -> platform.sendMessage(player, "你收到了一封新邮件，使用 /mail read 查看。"));
            return 1;
        }

        invocation.error("用法: " + usage());
        return 0;
    }

}
