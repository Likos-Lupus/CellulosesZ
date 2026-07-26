package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    public String usage() {
        return "/socialspy [player] [on|off]";
    }

    @Override
    public String name() {
        return "socialspy";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        final UUID target;
        final String targetName;

        var self = platform.player(invocation);
        if (args.length == 0 || isBoolean(args[0])) {
            if (self.isEmpty()) {
                invocation.errorKey("commands.messaging.socialspy-player-required");
                return 0;
            }

            target = self.get().uuid();
            targetName = self.get().name();
        } else {
            if (!invocation.hasPermission("cellulosesz.messaging.socialspy.others")) {
                invocation.errorKey("common.no-permission");
                return 0;
            }

            var resolved = uuid(invocation, args[0]);
            if (resolved.isEmpty()) return 0;

            target = resolved.get();
            targetName = args[0];
        }

        var valueIndex = args.length == 0 || isBoolean(args[0]) ? 0 : 1;
        final Optional<Boolean> requested;
        if (args.length > valueIndex) {
            requested = parseBoolean(args[valueIndex]);
            if (requested.isEmpty()) {
                invocation.errorKey("commands.common.invalid-boolean");
                return 0;
            }
        } else {
            requested = Optional.empty();
        }

        var current = requested.isPresent()
                ? java.util.concurrent.CompletableFuture.completedFuture(!requested.orElseThrow())
                : privateMessages.socialSpy(target);
        current.whenComplete((currentValue, loadFailure) -> {
            if (loadFailure != null) {
                invocation.errorKey("commands.messaging.preference-save-failed");
                return;
            }
            var enabled = requested.orElse(!currentValue);
            privateMessages.setSocialSpy(target, enabled).whenComplete((_, saveFailure) -> {
                if (saveFailure != null) invocation.errorKey("commands.messaging.preference-save-failed");
                else invocation.replyKey(
                        enabled ? "commands.messaging.social-spy-enabled" : "commands.messaging.social-spy-disabled",
                        Map.of("player", targetName)
                );
            });
        });
        return 1;
    }

    private static boolean isBoolean(String value) {
        return parseBoolean(value).isPresent();
    }

    private static Optional<Boolean> parseBoolean(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "on", "true", "enable", "enabled" -> Optional.of(Boolean.TRUE);
            case "off", "false", "disable", "disabled" -> Optional.of(Boolean.FALSE);
            default -> Optional.empty();
        };
    }

}
