package top.likoslupus.cellulosesz.modules.messaging.service;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserUpdate;
import top.likoslupus.cellulosesz.common.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.modules.messaging.domain.MessageResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class DefaultPrivateMessageService implements PrivateMessageService {

    private final PlayerDirectory players;
    private final PlayerAudienceService audiences;
    private final ServerThreadExecutor serverThread;
    private final UserService users;
    private final PermissionService permissions;
    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;

    public DefaultPrivateMessageService(
            PlayerDirectory players,
            PlayerAudienceService audiences,
            ServerThreadExecutor serverThread,
            UserService users,
            PermissionService permissions,
            DisplayNameService displayNames,
            MessageRenderer renderer
    ) {
        this.players = requireNonNull(players, "players");
        this.audiences = requireNonNull(audiences, "audiences");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.users = requireNonNull(users, "users");
        this.permissions = requireNonNull(permissions, "permissions");
        this.displayNames = requireNonNull(displayNames, "displayNames");
        this.renderer = requireNonNull(renderer, "renderer");
    }

    @Override
    public CompletableFuture<MessageResult> send(
            CellPlayer sender,
            CellPlayer target,
            String message
    ) {
        requireNonNull(sender, "sender");
        requireNonNull(target, "target");

        if (message.isBlank()) {
            return CompletableFuture.completedFuture(MessageResult.failure(
                    "service.messaging.empty-message"
            ));
        }

        return users
                .load(target.uuid())
                .thenCompose(targetUser -> {
                    if (targetUser.relations().ignored().contains(sender.uuid())) {
                        return CompletableFuture.completedFuture(MessageResult.failure(
                                "service.messaging.ignored"
                        ));
                    }

                    if (!targetUser.preferences().privateMessages()) {
                        return CompletableFuture.completedFuture(MessageResult.failure(
                                "service.messaging.private-messages-disabled"
                        ));
                    }

                    return updateReplyTargets(sender.uuid(), target.uuid())
                            .thenCompose(_ -> deliver(
                                    sender.uuid(),
                                    target.uuid(),
                                    target.name(),
                                    message
                            ));
                })
                .exceptionally(_ -> MessageResult.failed(
                        "service.messaging.persistence-failed"
                ));
    }

    private CompletableFuture<Void> updateReplyTargets(UUID sender, UUID target) {
        return users
                .update(
                        sender,
                        user -> {
                            var previous = user.preferences().outgoingReplyTarget();
                            return UserUpdate.of(
                                    user.withPreferences(user
                                            .preferences()
                                            .withOutgoingReplyTarget(target)),
                                    previous
                            );
                        }
                )
                .thenCompose(previousOutgoing -> users
                        .update(
                                target,
                                user -> {
                                    var previous = user.preferences().incomingReplyTarget();
                                    return UserUpdate.of(
                                            user.withPreferences(user
                                                    .preferences()
                                                    .withIncomingReplyTarget(sender)),
                                            previous
                                    );
                                }
                        )
                        .handle((previousIncoming, failure) ->
                                new TargetUpdate(
                                        (UUID) previousOutgoing,
                                        (UUID) previousIncoming,
                                        failure
                                )
                        )
                )
                .thenCompose(update -> {
                    if (update.failure() == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return users
                            .updateVoid(
                                    sender,
                                    user ->
                                            target.equals(user.preferences().outgoingReplyTarget())
                                                    ?
                                                    user.withPreferences(user
                                                            .preferences()
                                                            .withOutgoingReplyTarget(
                                                                    update.previousOutgoing()
                                                            ))
                                                    : user
                            )
                            .handle((_, rollbackFailure) -> {
                                if (rollbackFailure != null) {
                                    update.failure().addSuppressed(rollbackFailure);
                                }
                                throw new CompletionException(update.failure());
                            });
                });
    }

    private CompletableFuture<MessageResult> deliver(
            UUID senderUuid,
            UUID targetUuid,
            String targetName,
            String message
    ) {
        return serverThread
                .submit(() -> {
                    var senderPlayer = players.onlinePlayer(senderUuid);
                    var targetPlayer = players.onlinePlayer(targetUuid);

                    if (senderPlayer == null || targetPlayer == null) {
                        return Optional.<DeliveredNames>empty();
                    }


                    audiences.send(
                            targetPlayer,
                            renderer.render(
                                    audiences.locale(targetPlayer),
                                    "messaging.private-incoming",
                                    MessageArguments.builder()
                                            .add(displayNames.displayName(senderPlayer))
                                            .add(message)
                                            .build()
                            )
                    );
                    audiences.send(
                            senderPlayer,
                            renderer.render(
                                    audiences.locale(senderPlayer),
                                    "messaging.private-outgoing",
                                    MessageArguments.builder()
                                            .add(displayNames.displayName(targetPlayer))
                                            .add(message)
                                            .build()
                            )
                    );

                    return Optional.of(new DeliveredNames(
                            displayNames.plainDisplayName(senderPlayer),
                            displayNames.plainDisplayName(targetPlayer)
                    ));
                })
                .thenCompose(names ->
                        names.isEmpty()
                                ?
                                CompletableFuture.completedFuture(MessageResult.failure(
                                        "service.messaging.player-offline",
                                        MessageArguments.builder().add(targetName).build()
                                ))
                                : broadcastSpy(
                                        names.orElseThrow().sender(),
                                        names.orElseThrow().target(),
                                        message,
                                        Set.of(senderUuid, targetUuid)
                                ).handle((_, _) ->
                                        MessageResult.success("service.messaging.sent")
                                )
                );
    }

    @Override
    public CompletableFuture<Optional<UUID>> lastReplyTarget(UUID uuid) {
        return users
                .load(uuid)
                .thenApply(user -> {
                    var preferred = user.preferences().replyToLastRecipient()
                            ? user.preferences().outgoingReplyTarget()
                            : user.preferences().incomingReplyTarget();
                    var fallback = user.preferences().replyToLastRecipient()
                            ? user.preferences().incomingReplyTarget()
                            : user.preferences().outgoingReplyTarget();

                    return Optional.ofNullable(preferred != null
                            ? preferred
                            : fallback);
                });
    }

    @Override
    public CompletableFuture<Void> setLastReplyTarget(UUID uuid, UUID target) {
        return users.updateVoid(
                uuid,
                user -> user.withPreferences(user.preferences().withIncomingReplyTarget(target))
        );
    }

    @Override
    public CompletableFuture<Boolean> ignored(UUID viewer, UUID target) {
        return users
                .load(viewer)
                .thenApply(user -> user.relations().ignored().contains(target));
    }

    @Override
    public CompletableFuture<Void> setIgnored(
            UUID viewer,
            UUID target,
            boolean ignored
    ) {
        return users.updateVoid(
                viewer,
                user -> {
                    var updated = new LinkedHashSet<>(user.relations().ignored());
                    if (ignored) {
                        updated.add(target);
                    } else {
                        updated.remove(target);
                    }
                    return user.withRelations(user.relations().withIgnored(updated));
                }
        );
    }

    @Override
    public CompletableFuture<Boolean> socialSpy(UUID uuid) {
        return users
                .load(uuid)
                .thenApply(user -> user.preferences().socialSpy());
    }

    @Override
    public CompletableFuture<Void> setSocialSpy(UUID uuid, boolean enabled) {
        return users.updateVoid(
                uuid,
                user -> user.withPreferences(user.preferences().withSocialSpy(enabled))
        );
    }

    @Override
    public CompletableFuture<Void> broadcastSpy(
            String sender,
            String target,
            String message,
            Collection<UUID> excluded
    ) {
        var excludedSnapshot = Set.copyOf(excluded);
        return serverThread
                .submit(() -> players.onlinePlayers().stream()
                        .filter(player -> !excludedSnapshot.contains(player.uuid()))
                        .filter(player -> permissions.has(
                                player,
                                "cellulosesz.messaging.socialspy"
                        ))
                        .toList()
                )
                .thenCompose(candidates -> {
                    var checks = candidates.stream()
                            .map(player -> socialSpy(player.uuid())
                                    .thenApply(enabled -> new SpyCandidate(player, enabled))
                            )
                            .toList();
                    return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
                            .thenCompose(_ -> serverThread
                                    .submit(() -> {
                                        checks.stream()
                                                .map(check -> check.getNow(
                                                        new SpyCandidate(null, false)
                                                ))
                                                .filter(candidate -> candidate.enabled()
                                                        && candidate.player() != null
                                                )
                                                .forEach(candidate -> audiences.send(
                                                        candidate.player(),
                                                        renderer.render(
                                                                audiences.locale(candidate.player()),
                                                                "messaging.social-spy",
                                                                MessageArguments.builder()
                                                                        .add(sender)
                                                                        .add(target)
                                                                        .add(message)
                                                                        .build()
                                                        )
                                                ));
                                        return Boolean.TRUE;
                                    })
                            )
                            .thenAccept(_ -> {
                            });
                });
    }

    private record DeliveredNames(
            String sender,
            String target
    ) {

    }

    private record TargetUpdate(
            @Nullable UUID previousOutgoing,
            @Nullable UUID previousIncoming,
            @Nullable Throwable failure
    ) {

    }

    private record SpyCandidate(
            @Nullable CellPlayer player,
            boolean enabled
    ) {

    }

}
