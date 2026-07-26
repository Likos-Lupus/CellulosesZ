package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.List;
import java.util.Map;

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
            invocation.errorKey(
                    "commands.messaging.reply-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var message = join(args, 0);
        if (!validLength(invocation, message)) return 0;

        privateMessages.lastReplyTarget(self.orElseThrow().uuid()).whenComplete((targetUuid, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.messaging.persistence-failed");
                return;
            }
            if (targetUuid.isEmpty()) {
                invocation.errorKey("commands.messaging.reply-command.error.2");
                return;
            }
            platform.callOnServerThread(() -> invocation.resolvePlayer(targetUuid.orElseThrow().toString()).online())
                    .whenComplete((target, resolveFailure) -> {
                        if (resolveFailure != null || target.isEmpty()) {
                            invocation.errorKey("commands.messaging.reply-command.error.3");
                            return;
                        }
                        privateMessages.send(self.orElseThrow(), target.orElseThrow(), message)
                                .whenComplete((result, sendFailure) -> {
                                    if (sendFailure != null)
                                        invocation.errorKey("service.messaging.persistence-failed");
                                    else if (!result.success()) invocation.error(result.message());
                                });
                    });
        });
        return 1;
    }

}
