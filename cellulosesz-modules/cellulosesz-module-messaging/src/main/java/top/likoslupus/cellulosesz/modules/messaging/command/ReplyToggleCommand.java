package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ReplyToggleCommand extends AbstractMessagingCommand {

    public ReplyToggleCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config
    ) {
        super(platform, users, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.rtoggle";
    }

    @Override
    public String usage() {
        return "/rtoggle [player] [on|off]";
    }

    @Override
    public String name() {
        return "rtoggle";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length > 2) return usageError(invocation);

        UUID uuid;
        String playerName;
        String mode = "";
        if (args.length == 0 || (args.length == 1 && isMode(args[0]))) {
            var self = player(invocation);

            if (self.isEmpty()) {
                return 0;
            }

            uuid = self.get().uuid();
            playerName = self.get().name();
            if (args.length == 1) {
                mode = args[0];
            }
        } else {
            if (!invocation.hasPermission("cellulosesz.messaging.rtoggle.others")) {
                invocation.errorKey("commands.messaging.reply-toggle.others");
                return 0;
            }

            var resolved = invocation.resolvePlayer(args[0]);
            if (resolved.optionalUuid().isEmpty()) {
                invocation.errorKey(
                        "commands.messaging.reply-toggle.player",
                        Map.of("player", args[0])
                );
                return 0;
            }

            uuid = resolved.optionalUuid().orElseThrow();
            playerName = resolved.name();
            if (args.length == 2) {
                mode = args[1];
            }
        }

        if (!mode.isBlank() && !isMode(mode)) {
            return usageError(invocation);
        }

        var user = users.load(uuid).join();
        var enabled = mode.isBlank()
                ? !user.preferences.replyToLastRecipient
                : enabled(mode);
        var previous = user.preferences.replyToLastRecipient;
        user.preferences.replyToLastRecipient = enabled;
        users.markDirty(uuid);
        try {
            users.save(uuid).join();
        } catch (RuntimeException _) {
            user.preferences.replyToLastRecipient = previous;
            users.markDirty(uuid);
            invocation.errorKey("service.user.persistence-failed");
            return 0;
        }

        var changedOther = platform.player(invocation)
                .map(actor -> !actor.uuid().equals(uuid))
                .orElse(true);

        String key = enabled ? (
                changedOther
                ? "commands.messaging.reply-toggle.recipient-other"
                        : "commands.messaging.reply-toggle.recipient"
        ) : (
                changedOther
                ? "commands.messaging.reply-toggle.sender-other"
                        : "commands.messaging.reply-toggle.sender"
        );

        invocation.replyKey(key, Map.of("player", playerName));
        return 1;
    }

    private int usageError(CommandInvocation invocation) {
        invocation.errorKey(
                "commands.messaging.reply-toggle.usage",
                Map.of("usage", usage())
        );
        return 0;
    }

    private boolean isMode(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true", "enable", "off", "false", "disable" -> true;
            default -> false;
        };
    }

    private boolean enabled(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true", "enable" -> true;
            default -> false;
        };
    }

}
