package top.likoslupus.cellulosesz.modules.messaging.application;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.messaging.MailMessage;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class MailCommandService {

    private final MailService mail;
    private final UserService users;
    private final PlayerResolver resolver;
    private final PlayerDirectory players;
    private final PlayerAudienceService audiences;
    private final ServerThreadExecutor serverThread;
    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;
    private final PrivateMessageService privateMessages;
    private final MessagingConfig config;

    public MailCommandService(
            MailService mail,
            UserService users,
            PlayerResolver resolver,
            PlayerDirectory players,
            PlayerAudienceService audiences,
            ServerThreadExecutor serverThread,
            DisplayNameService displayNames,
            MessageRenderer renderer,
            PrivateMessageService privateMessages,
            MessagingConfig config
    ) {
        this.mail = requireNonNull(mail, "mail");
        this.users = requireNonNull(users, "users");
        this.resolver = requireNonNull(resolver, "resolver");
        this.players = requireNonNull(players, "players");
        this.audiences = requireNonNull(audiences, "audiences");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.displayNames = requireNonNull(displayNames, "displayNames");
        this.renderer = requireNonNull(renderer, "renderer");
        this.privateMessages = requireNonNull(privateMessages, "privateMessages");
        this.config = requireNonNull(config, "config");
    }

    public CompletableFuture<Result> read(UUID recipient, int page) {
        return mail.inbox(recipient)
                .thenCompose(messages -> {
                    if (messages.isEmpty()) {
                        return CompletableFuture.completedFuture(Result.success(LocalizedMessage.of("commands.messaging.mail-empty")));
                    }

                    var pages = Math.toIntExact(((long) messages.size() + config.mailPageSize - 1L) / config.mailPageSize);
                    if (page > pages) {
                        return CompletableFuture.completedFuture(Result.failure(LocalizedMessage.of(
                                "commands.common.page-out-of-range",
                                Map.of("pages", pages)
                        )));
                    }

                    var start = Math.multiplyExact(page - 1, config.mailPageSize);
                    var end = (int) Math.min(
                            (long) start + config.mailPageSize,
                            messages.size()
                    );
                    var selected = messages.subList(start, end);
                    var response = new ArrayList<LocalizedMessage>();

                    response.add(LocalizedMessage.of(
                            "commands.messaging.mail-page-header",
                            Map.of(
                                    "page", page,
                                    "pages", pages,
                                    "unread", messages.stream()
                                            .filter(message -> !message.read())
                                            .count()
                            )
                    ));

                    var formatter = dateFormatter();
                    var readIds = new ArrayList<UUID>();
                    selected.forEach(message -> {
                        response.add(LocalizedMessage.of(
                                "commands.messaging.mail-entry",
                                Map.of(
                                        "id", message.id(),
                                        "unread", !message.read(),
                                        "time", formatter.format(Instant.ofEpochMilli(message.sentAt())),
                                        "sender", message.fromName(),
                                        "message", message.message()
                                )
                        ));
                        if (!message.read()) {
                            readIds.add(message.id());
                        }
                    });

                    return readIds.isEmpty()
                            ? CompletableFuture.completedFuture(new Result(true, response))
                            : mail.markRead(recipient, readIds)
                                    .thenApply(_ -> new Result(true, response));
                });
    }

    private DateTimeFormatter dateFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.of(config.mailTimeZone));
    }

    public CompletableFuture<Result> unread(UUID recipient) {
        return mail.unreadCount(recipient)
                .thenApply(count -> Result.success(LocalizedMessage.of(
                        "commands.messaging.mail-unread",
                        Map.of("count", count)
                )));
    }

    public CompletableFuture<Result> delete(UUID recipient, UUID id) {
        return mail.delete(recipient, id)
                .thenApply(removed ->
                        removed ? Result.success(LocalizedMessage.of(
                                "commands.messaging.mail-deleted",
                                Map.of("id", id)
                        )) : Result.failure(LocalizedMessage.of(
                                "commands.messaging.mail-not-found",
                                Map.of("id", id)
                        )));
    }

    public CompletableFuture<Result> clear(UUID recipient) {
        return mail.clear(recipient).thenApply(count -> Result.success(LocalizedMessage.of(
                "commands.messaging.mail-cleared",
                Map.of("count", count)
        )));
    }

    public CompletableFuture<Result> send(
            Optional<CellPlayer> sender,
            String targetToken,
            Optional<Duration> duration,
            String body
    ) {
        var invalid = validate(body);
        if (invalid.isPresent()) {
            return CompletableFuture.completedFuture(Result.failure(invalid.orElseThrow()));
        }

        if (sender.isEmpty() && !config.allowConsolePrivateMessage) {
            return CompletableFuture.completedFuture(Result.failure(LocalizedMessage.of(
                    "commands.messaging.console-disabled"
            )));
        }

        return resolver.resolve(targetToken, sender.orElse(null))
                .thenCompose(target -> {
                    if (target.optionalUuid().isEmpty()) {
                        return CompletableFuture.completedFuture(Result.failure(LocalizedMessage.of(
                                "commands.common.player-not-found",
                                Map.of("player", targetToken)
                        )));
                    }

                    var now = System.currentTimeMillis();
                    final Long expiresAt;
                    try {
                        expiresAt = duration
                                .map(value -> {
                                    if (value.compareTo(Duration.ofSeconds(config.maximumTemporaryMailSeconds)) > 0) {
                                        throw new IllegalArgumentException("duration exceeds maximum");
                                    }
                                    return Math.addExact(now, value.toMillis());
                                })
                                .orElse(null);
                    } catch (RuntimeException _) {
                        return CompletableFuture.completedFuture(Result.failure(LocalizedMessage.of(
                                "commands.messaging.mail-invalid-duration",
                                Map.of("maximum", config.maximumTemporaryMailSeconds)
                        )));
                    }

                    var senderName = sender
                            .map(displayNames::plainDisplayName)
                            .orElse("console");
                    var recipient = target.optionalUuid().orElseThrow();
                    var message = new MailMessage(
                            UUID.randomUUID(),
                            sender.map(CellPlayer::uuid).orElse(null),
                            senderName,
                            recipient,
                            body,
                            now,
                            expiresAt,
                            false
                    );
                    return mail.send(message)
                            .thenCompose(_ -> notifyOnline(recipient))
                            .thenCompose(_ ->
                                    privateMessages.broadcastSpy(
                                            senderName,
                                            "mail:" + target.name(),
                                            body,
                                            sender.map(value -> Set.of(value.uuid(), recipient))
                                                    .orElseGet(() -> Set.of(recipient))
                                    ).handle((ignored, _) ->
                                            Result.success(LocalizedMessage.of(
                                                    "commands.messaging.mail-sent",
                                                    Map.of("player", target.name())
                                            ))
                                    )
                            );
                });
    }

    private Optional<LocalizedMessage> validate(String body) {
        if (body.isBlank()) {
            return Optional.of(LocalizedMessage.of("service.messaging.empty-message"));
        }
        if (body.length() > config.maxMessageLength) {
            return Optional.of(LocalizedMessage.of(
                    "commands.messaging.message-too-long",
                    Map.of("maximum", config.maxMessageLength)
            ));
        }
        return Optional.empty();
    }

    private CompletableFuture<Void> notifyOnline(UUID recipient) {
        return serverThread
                .submit(() -> {
                    players.onlinePlayer(recipient)
                            .ifPresent(player -> audiences.send(
                                    player,
                                    renderer.render(audiences.locale(player), "messaging.mail-received")
                            ));
                    return Boolean.TRUE;
                })
                .thenAccept(_ -> {
                });
    }

    public CompletableFuture<Result> sendAll(Optional<CellPlayer> sender, String body) {
        var invalid = validate(body);
        if (invalid.isPresent()) {
            return CompletableFuture.completedFuture(Result.failure(invalid.orElseThrow()));
        }

        var recipients = new LinkedHashSet<>(users.knownUuids());
        players.onlinePlayers()
                .forEach(player -> recipients.add(player.uuid()));
        if (recipients.size() > config.maxSendAllRecipients) {
            return CompletableFuture.completedFuture(Result.failure(LocalizedMessage.of(
                    "commands.messaging.mail-sendall-too-many",
                    Map.of(
                            "count", recipients.size(),
                            "maximum", config.maxSendAllRecipients
                    )
            )));
        }

        var now = System.currentTimeMillis();
        var senderName = sender
                .map(displayNames::plainDisplayName)
                .orElse("console");
        return mail
                .sendAll(recipients, recipient ->
                        new MailMessage(
                                UUID.randomUUID(),
                                sender.map(CellPlayer::uuid).orElse(null),
                                senderName,
                                recipient,
                                body,
                                now,
                                null,
                                false
                        )
                )
                .thenCompose(count ->
                        serverThread.submit(() -> {
                            players.onlinePlayers().forEach(player ->
                                    audiences.send(
                                            player,
                                            renderer.render(audiences.locale(player), "messaging.mail-received")
                                    ));
                            return count;
                        })
                )
                .thenApply(count -> Result.success(LocalizedMessage.of(
                        "commands.messaging.mail-sent-all",
                        Map.of("count", count)
                )));
    }

    public record Result(
            boolean success,
            List<LocalizedMessage> messages
    ) {

        public Result {
            messages = List.copyOf(messages);
        }

        public static Result success(LocalizedMessage message) {
            return new Result(true, List.of(message));
        }

        public static Result failure(LocalizedMessage message) {
            return new Result(false, List.of(message));
        }

    }

}
