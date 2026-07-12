package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.messaging.MailMessage;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Map;
import java.util.UUID;

public final class MailCommand extends AbstractMessagingCommand {

    private final MailService mail;
    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;

    public MailCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            MailService mail,
            DisplayNameService displayNames,
            MessageRenderer renderer
    ) {
        super(platform, users, config);
        this.mail = mail;
        this.displayNames = displayNames;
        this.renderer = renderer;
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
                    invocation.replyKey("commands.messaging.mail-command.reply.1");
                } else {
                    var entries = new StringBuilder();
                    messages.forEach(message -> entries.append("\n- ")
                            .append(message.fromName)
                            .append(": ")
                            .append(message.message));
                    invocation.replyKey(
                            "commands.messaging.mail-list",
                            Map.of("entries", entries.toString())
                    );
                    mail.markRead(self.get().uuid());
                }
            });
            return 1;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            mail.clear(self.get().uuid())
                    .thenRun(() -> invocation.replyKey("commands.messaging.mail-command.reply.2"));
            return 1;
        }

        if (args[0].equalsIgnoreCase("send")) {
            if (args.length < 3) {
                invocation.errorKey("commands.messaging.mail-command.error.1");
                return 0;
            }

            var recipient = uuid(invocation, args[1]);
            var messageText = join(args, 2);
            if (recipient.isEmpty() || !validLength(invocation, messageText)) return 0;

            var message = new MailMessage();
            message.id = UUID.randomUUID().toString();
            message.fromUuid = self.get().uuid();
            message.fromName = displayNames.plainDisplayName(self.get());
            message.toUuid = recipient.get();
            message.message = messageText;
            mail.send(recipient.get(), message)
                    .thenRun(() -> invocation.replyKey(
                            "commands.messaging.mail-command.reply.3",
                            Map.of("value0", args[1])
                    ));
            invocation.resolvePlayer(args[1]).online()
                    .ifPresent(player -> platform.sendMessage(
                            player,
                            renderer.render(platform.locale(player), "messaging.mail-received")
                    ));
            return 1;
        }

        invocation.errorKey(
                "commands.messaging.mail-command.error.2",
                Map.of("value0", usage())
        );
        return 0;
    }

}
