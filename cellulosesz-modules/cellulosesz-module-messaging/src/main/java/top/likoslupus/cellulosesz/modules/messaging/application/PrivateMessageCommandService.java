package top.likoslupus.cellulosesz.modules.messaging.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserUpdate;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;
import top.likoslupus.cellulosesz.modules.messaging.domain.MessageResult;
import top.likoslupus.cellulosesz.modules.messaging.service.PrivateMessageService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class PrivateMessageCommandService {

    private final PlayerResolver resolver;
    private final PlayerDirectory players;
    private final NameCacheService names;
    private final PrivateMessageService privateMessages;
    private final UserService users;
    private final ServerThreadExecutor serverThread;
    private volatile MessagingConfig config;

    public PrivateMessageCommandService(
            PlayerResolver resolver,
            PlayerDirectory players,
            NameCacheService names,
            PrivateMessageService privateMessages,
            UserService users,
            ServerThreadExecutor serverThread,
            MessagingConfig config
    ) {
        this.resolver = requireNonNull(resolver, "resolver");
        this.players = requireNonNull(players, "players");
        this.names = requireNonNull(names, "names");
        this.privateMessages = requireNonNull(privateMessages, "privateMessages");
        this.users = requireNonNull(users, "users");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.config = requireNonNull(config, "config").validatedCopy();
    }

    public void configure(MessagingConfig config) {
        this.config = requireNonNull(config, "config").validatedCopy();
    }

    public CompletableFuture<MessageResult> send(
            CellPlayer sender,
            String targetToken,
            String message
    ) {
        var invalid = validate(message);
        if (invalid.isPresent()) {
            return CompletableFuture.completedFuture(invalid.orElseThrow());
        }

        return resolver
                .resolve(targetToken, sender)
                .thenCompose(target -> {
                    if (target.state() != ResolvedPlayerState.ONLINE
                            || target.onlinePlayer() == null
                    ) {
                        return CompletableFuture.completedFuture(MessageResult.failure(
                                "service.messaging.player-offline",
                                MessageArguments.builder().add(targetToken).build()
                        ));
                    }
                    if (target.uuid().equals(sender.uuid())) {
                        return CompletableFuture.completedFuture(MessageResult.failure(
                                "commands.messaging.cannot-message-self"
                        ));
                    }
                    return privateMessages.send(
                            sender,
                            target.onlinePlayer(),
                            message
                    );
                });
    }

    private Optional<MessageResult> validate(String message) {
        if (message.isBlank()) {
            return Optional.of(MessageResult.failure("service.messaging.empty-message"));
        }
        if (message.length() > config.maxMessageLength) {
            return Optional.of(MessageResult.failure(
                    "commands.messaging.message-too-long",
                    MessageArguments.builder().add(config.maxMessageLength).build()
            ));
        }
        return Optional.empty();
    }

    public CompletableFuture<MessageResult> reply(CellPlayer sender, String message) {
        var invalid = validate(message);
        if (invalid.isPresent()) {
            return CompletableFuture.completedFuture(invalid.orElseThrow());
        }

        return privateMessages
                .lastReplyTarget(sender.uuid())
                .thenCompose(target -> {
                    if (target.isEmpty()) {
                        return CompletableFuture.completedFuture(MessageResult.failure(
                                "commands.messaging.reply-command.error.there-no-player-reply"
                        ));
                    }
                    return serverThread
                            .submit(() -> players.onlinePlayer(target.orElseThrow()))
                            .thenCompose(online ->
                                    online == null
                                            ?
                                            CompletableFuture.completedFuture(MessageResult.failure(
                                                    "commands.messaging.reply-command.error.player-can-reply-no-longer-online"
                                            ))
                                            : privateMessages.send(
                                                    sender,
                                                    online,
                                                    message
                                            ));
                });
    }

    public CompletableFuture<MessageResult> toggleMessages(UUID uuid) {
        return users
                .update(
                        uuid,
                        user -> {
                            var enabled = !user.preferences().privateMessages();
                            return UserUpdate.of(
                                    user.withPreferences(user
                                            .preferences()
                                            .withPrivateMessages(enabled)),
                                    enabled
                            );
                        }
                )
                .thenApply(enabled -> MessageResult.success(
                        enabled
                                ? "commands.messaging.private-messages-enabled"
                                : "commands.messaging.private-messages-disabled"
                ));
    }

    public CompletableFuture<MessageResult> ignore(CellPlayer actor, String targetToken) {
        return resolver
                .resolve(targetToken, actor)
                .thenCompose(target -> {
                    if (target.uuid() == null) {
                        return CompletableFuture.completedFuture(MessageResult.failure(
                                "commands.common.player-not-found",
                                MessageArguments.builder().add(targetToken).build()
                        ));
                    }

                    var targetUuid = target.uuid();
                    if (targetUuid.equals(actor.uuid())) {
                        return CompletableFuture.completedFuture(MessageResult.failure(
                                "commands.messaging.ignore-self"
                        ));
                    }

                    return privateMessages
                            .ignored(actor.uuid(), targetUuid)
                            .thenCompose(current -> privateMessages
                                    .setIgnored(
                                            actor.uuid(),
                                            targetUuid,
                                            !current
                                    )
                                    .thenApply(_ -> MessageResult.success(
                                            current
                                                    ? "commands.messaging.ignore-disabled"
                                                    : "commands.messaging.ignore-enabled",
                                            MessageArguments.builder()
                                                    .add(target.name())
                                                    .build()
                                    ))
                            );
                });
    }

    public CompletableFuture<MessageResult> replyPreference(
            UUID target,
            String targetName,
            Optional<Boolean> requested
    ) {
        return users
                .update(
                        target,
                        user -> {
                            var enabled = requested
                                    .orElse(!user.preferences().replyToLastRecipient());
                            return UserUpdate.of(
                                    user.withPreferences(
                                            user.preferences()
                                                    .withReplyToLastRecipient(enabled)
                                    ),
                                    enabled
                            );
                        }
                )
                .thenApply(enabled -> MessageResult.success(
                        enabled
                                ? "commands.messaging.reply-toggle.recipient"
                                : "commands.messaging.reply-toggle.sender",
                        MessageArguments.empty()
                ));
    }

    public CompletableFuture<MessageResult> socialSpy(
            UUID target,
            String targetName,
            Optional<Boolean> requested
    ) {
        return users
                .update(
                        target,
                        user -> {
                            var enabled = requested
                                    .orElse(!user.preferences().socialSpy());
                            return UserUpdate.of(
                                    user.withPreferences(user.preferences().withSocialSpy(enabled)),
                                    enabled
                            );
                        }
                )
                .thenApply(enabled -> MessageResult.success(
                        enabled
                                ? "commands.messaging.social-spy-enabled"
                                : "commands.messaging.social-spy-disabled",
                        MessageArguments.empty()
                ));
    }

    public CompletableFuture<ResolvedTarget> knownTarget(String token, CellPlayer viewer) {
        return resolver
                .resolve(token, viewer)
                .thenApply(result -> {
                    if (result.uuid() == null) {
                        throw new IllegalArgumentException("Unknown player: " + token);
                    }

                    return new ResolvedTarget(result.uuid(), result.name());
                });
    }

    public List<String> knownNames() {
        return names.entries().values().stream()
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> onlineNames(CellPlayer viewer) {
        return players.onlinePlayers().stream()
                .filter(player -> !player.uuid().equals(viewer.uuid()))
                .map(CellPlayer::name)
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }

    public record ResolvedTarget(
            UUID uuid,
            String name
    ) {

    }

}
