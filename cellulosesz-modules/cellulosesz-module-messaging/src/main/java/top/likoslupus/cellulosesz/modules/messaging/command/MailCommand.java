package top.likoslupus.cellulosesz.modules.messaging.command;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.messaging.MailMessage;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;
import top.likoslupus.cellulosesz.modules.messaging.service.MailDurationParser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MailCommand extends AbstractMessagingCommand {

    private final MailService mail;
    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;
    private final PrivateMessageService privateMessages;

    public MailCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            MailService mail,
            DisplayNameService displayNames,
            MessageRenderer renderer,
            PrivateMessageService privateMessages
    ) {
        super(platform, users, config);
        this.mail = mail;
        this.displayNames = displayNames;
        this.renderer = renderer;
        this.privateMessages = privateMessages;
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.mail";
    }

    @Override
    public String usage() {
        return "/mail <read [page]|unread|delete <id>|clear|send <player> <message>|sendtemp <player> <duration> <message>|sendall <message>>";
    }

    @Override
    public String name() {
        return "mail";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        var subcommand = args.length == 0
                ? "read"
                : args[0].toLowerCase(java.util.Locale.ROOT);

        return switch (subcommand) {
            case "read" -> read(invocation, args);
            case "unread" -> unread(invocation);
            case "delete" -> delete(invocation, args);
            case "clear" -> clear(invocation);
            case "send" -> send(invocation, args, null);
            case "sendtemp" -> sendTemporary(invocation, args);
            case "sendall" -> sendAll(invocation, args);
            default -> {
                invocation.errorKey(
                        "commands.messaging.mail-usage",
                        Map.of("usage", usage())
                );
                yield 0;
            }
        };
    }

    private int read(CommandInvocation invocation, String[] args) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException _) {
                invocation.errorKey("commands.common.invalid-page");
                return 0;
            }
        }

        if (page < 1) {
            invocation.errorKey("commands.common.invalid-page");
            return 0;
        }

        var requestedPage = page;
        mail.inbox(self.get().uuid()).whenComplete((messages, failure) -> {
            if (failure != null) {
                invocation.errorKey("commands.messaging.mail-storage-failed");
                return;
            }

            if (messages.isEmpty()) {
                invocation.replyKey("commands.messaging.mail-empty");
                return;
            }

            var pageSize = Math.max(1, config.mailPageSize);
            var pages = Math.max(1, (messages.size() + pageSize - 1) / pageSize);
            if (requestedPage > pages) {
                invocation.errorKey(
                        "commands.common.page-out-of-range",
                        Map.of("pages", pages)
                );
                return;
            }

            var start = (requestedPage - 1) * pageSize;
            var end = Math.min(start + pageSize, messages.size());
            var visible = messages.subList(start, end);
            var entries = new StringBuilder();
            var readIds = new ArrayList<UUID>();
            visible.forEach(message -> {
                entries.append('\n')
                        .append(message.read() ? "  " : "* ")
                        .append(message.id()).append(" | ")
                        .append(dateFormatter().format(Instant.ofEpochMilli(message.sentAt())))
                        .append(" | ").append(message.fromName())
                        .append(": ").append(message.message());
                if (!message.read()) readIds.add(message.id());
            });

            invocation.replyKey(
                    "commands.messaging.mail-page",
                    Map.of(
                            "page", requestedPage,
                            "pages", pages,
                            "unread", messages.stream()
                                    .filter(message -> !message.read())
                                    .count(),
                            "entries", entries.toString()
                    )
            );

            if (!readIds.isEmpty()) {
                mail.markRead(self.get().uuid(), readIds).whenComplete((ignored, markFailure) -> {
                    if (markFailure != null) {
                        invocation.errorKey("commands.messaging.mail-mark-read-failed");
                    }
                });
            }
        });

        return 1;
    }

    private int unread(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        mail.unreadCount(self.get().uuid()).whenComplete((count, failure) -> {
            if (failure != null) {
                invocation.errorKey("commands.messaging.mail-storage-failed");
            } else {
                invocation.replyKey(
                        "commands.messaging.mail-unread",
                        Map.of("count", count)
                );
            }
        });
        return 1;
    }

    private int delete(CommandInvocation invocation, String[] args) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        if (args.length != 2) {
            invocation.errorKey(
                    "commands.messaging.mail-usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        final UUID id;
        try {
            id = UUID.fromString(args[1]);
        } catch (IllegalArgumentException _) {
            invocation.errorKey("commands.messaging.mail-invalid-id");
            return 0;
        }

        mail.delete(self.get().uuid(), id).whenComplete((removed, failure) -> {
            if (failure != null) {
                invocation.errorKey("commands.messaging.mail-storage-failed");
            } else if (!removed) {
                invocation.errorKey(
                        "commands.messaging.mail-not-found",
                        Map.of("id", id)
                );
            } else {
                invocation.replyKey(
                        "commands.messaging.mail-deleted",
                        Map.of("id", id)
                );
            }
        });
        return 1;
    }

    private int clear(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        mail.clear(self.get().uuid()).whenComplete((removed, failure) -> {
            if (failure != null) {
                invocation.errorKey("commands.messaging.mail-storage-failed");
            } else {
                invocation.replyKey(
                        "commands.messaging.mail-cleared",
                        Map.of("count", removed)
                );
            }
        });
        return 1;
    }

    private int send(
            CommandInvocation invocation,
            String[] args,
            @Nullable Long durationMillis
    ) {
        var textStart = durationMillis == null ? 2 : 3;
        if (args.length <= textStart) {
            invocation.errorKey("commands.messaging.mail-usage", Map.of("usage", usage()));
            return 0;
        }

        var recipient = uuid(invocation, args[1]);
        var text = join(args, textStart);
        if (recipient.isEmpty() || !validLength(invocation, text)) return 0;

        var sender = platform.player(invocation).orElse(null);
        if (sender == null && !config.allowConsolePrivateMessage) {
            invocation.errorKey("commands.messaging.console-disabled");
            return 0;
        }

        var now = System.currentTimeMillis();
        final Long expiresAt;
        try {
            expiresAt = durationMillis == null ? null : Math.addExact(now, durationMillis);
        } catch (ArithmeticException _) {
            invocation.errorKey("commands.messaging.mail-invalid-duration", Map.of("maximum", config.maximumTemporaryMailSeconds));
            return 0;
        }
        var senderName = sender == null ? "console" : displayNames.plainDisplayName(sender);
        var message = new MailMessage(
                UUID.randomUUID(),
                sender == null ? null : sender.uuid(),
                senderName,
                recipient.get(),
                text,
                now,
                expiresAt,
                false
        );

        mail.send(message).whenComplete((ignored, failure) -> {
            if (failure != null) {
                invocation.errorKey("commands.messaging.mail-storage-failed");
                return;
            }

            invocation.replyKey(
                    "commands.messaging.mail-sent",
                    Map.of("player", args[1])
            );
            platform.runOnServerThread(() -> invocation.resolvePlayer(args[1])
                    .online()
                    .ifPresent(player -> platform.sendMessage(
                            player,
                            renderer.render(platform.locale(player), "messaging.mail-received")
                    )));
            privateMessages.broadcastSpy(
                    senderName,
                    "mail:" + args[1],
                    text,
                    sender == null
                            ? Set.of(recipient.get())
                            : Set.of(sender.uuid(), recipient.get())
            );
        });
        return 1;
    }

    private int sendTemporary(CommandInvocation invocation, String[] args) {
        if (args.length < 4) {
            invocation.errorKey("commands.messaging.mail-usage", Map.of("usage", usage()));
            return 0;
        }

        var duration = MailDurationParser.parseMillis(args[2]);
        final long maximumMillis;
        try {
            maximumMillis = Math.multiplyExact(Math.max(1L, config.maximumTemporaryMailSeconds), 1000L);
        } catch (ArithmeticException _) {
            invocation.errorKey("commands.messaging.mail-invalid-duration", Map.of("maximum", config.maximumTemporaryMailSeconds));
            return 0;
        }
        if (duration.isEmpty() || duration.getAsLong() > maximumMillis) {
            invocation.errorKey(
                    "commands.messaging.mail-invalid-duration",
                    Map.of("maximum", config.maximumTemporaryMailSeconds)
            );
            return 0;
        }

        return send(invocation, args, duration.getAsLong());
    }

    private int sendAll(CommandInvocation invocation, String[] args) {
        if (!invocation.hasPermission("cellulosesz.messaging.mail.sendall")) {
            invocation.errorKey("common.no-permission");
            return 0;
        }

        if (args.length < 2) {
            invocation.errorKey(
                    "commands.messaging.mail-usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var text = join(args, 1);
        if (!validLength(invocation, text)) return 0;

        var sender = platform.player(invocation).orElse(null);
        var senderName = sender == null ? "console" : displayNames.plainDisplayName(sender);
        var senderUuid = sender == null ? null : sender.uuid();
        var recipients = new java.util.LinkedHashSet<>(users.knownUuids());
        platform.onlinePlayers().forEach(player -> recipients.add(player.uuid()));
        if (recipients.size() > config.maxSendAllRecipients) {
            invocation.errorKey("commands.messaging.mail-sendall-too-many", Map.of("count", recipients.size(), "maximum", config.maxSendAllRecipients));
            return 0;
        }
        var now = System.currentTimeMillis();
        mail.sendAll(recipients, recipient -> new MailMessage(
                UUID.randomUUID(),
                senderUuid,
                senderName,
                recipient,
                text,
                now,
                null,
                false
        )).whenComplete((count, failure) -> {
            if (failure != null) {
                invocation.errorKey("commands.messaging.mail-storage-failed");
                return;
            }

            invocation.replyKey(
                    "commands.messaging.mail-sent-all",
                    Map.of("count", count)
            );
            platform.runOnServerThread(() -> platform.onlinePlayers().forEach(player -> platform.sendMessage(
                    player,
                    renderer.render(platform.locale(player), "messaging.mail-received")
            )));
            privateMessages.broadcastSpy(
                    senderName,
                    "mail:all",
                    text,
                    sender == null
                            ? Set.of()
                            : Set.of(sender.uuid())
            );
        });
        return 1;
    }

    private DateTimeFormatter dateFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.of(config.mailTimeZone));
    }

}
