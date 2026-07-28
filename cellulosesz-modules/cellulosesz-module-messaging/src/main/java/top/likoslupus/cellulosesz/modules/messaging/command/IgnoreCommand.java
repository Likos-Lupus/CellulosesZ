package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Map;

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
            invocation.errorKey(
                    "commands.messaging.ignore-command.error.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var self = player(invocation);
        var target = uuid(invocation, args[0]);
        if (self.isEmpty() || target.isEmpty()) return 0;

        privateMessages.ignored(self.orElseThrow().uuid(), target.orElseThrow())
                .whenComplete((currentlyIgnored, loadFailure) -> {
                    if (loadFailure != null) {
                        invocation.errorKey("service.user.persistence-failed");
                        return;
                    }
                    var nowIgnored = !currentlyIgnored;
                    privateMessages.setIgnored(self.orElseThrow().uuid(), target.orElseThrow(), nowIgnored)
                            .whenComplete((_, saveFailure) -> {
                                if (saveFailure != null) invocation.errorKey("service.user.persistence-failed");
                                else invocation.replyKey(
                                        nowIgnored
                                                ? "commands.messaging.ignore-enabled"
                                                : "commands.messaging.ignore-disabled",
                                        Map.of("player", args[0])
                                );
                            });
                });
        return 1;
    }

}
