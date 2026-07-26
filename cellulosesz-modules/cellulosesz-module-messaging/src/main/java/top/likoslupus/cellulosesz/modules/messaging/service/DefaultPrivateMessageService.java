package top.likoslupus.cellulosesz.modules.messaging.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.messaging.MessageResult;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class DefaultPrivateMessageService implements PrivateMessageService {

    private final PlatformService platform;
    private final UserService users;
    private final PermissionService permissions;
    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;

    public DefaultPrivateMessageService(
            PlatformService platform,
            UserService users,
            PermissionService permissions,
            DisplayNameService displayNames,
            MessageRenderer renderer
    ) {
        this.platform = platform;
        this.users = users;
        this.permissions = permissions;
        this.displayNames = displayNames;
        this.renderer = renderer;
    }

    @Override
    public CompletableFuture<MessageResult> send(CellPlayer sender, CellPlayer target, String message) {
        requireNonNull(sender, "sender");
        requireNonNull(target, "target");
        if (message.isBlank())
            return CompletableFuture.completedFuture(MessageResult.failure("service.messaging.empty-message"));

        return users.load(target.uuid()).thenCompose(targetUser -> {
            if (targetUser.relations.ignored.contains(sender.uuid())) {
                return CompletableFuture.completedFuture(MessageResult.failure("service.messaging.ignored"));
            }
            if (!targetUser.preferences.privateMessages) {
                return CompletableFuture.completedFuture(MessageResult.failure("service.messaging.private-messages-disabled"));
            }
            return users.update(sender.uuid(), user -> {
                        var previous = user.preferences.outgoingReplyTarget;
                        user.preferences.outgoingReplyTarget = target.uuid();
                        return previous;
                    }).thenCompose(previousOutgoing -> users.update(target.uuid(), user -> {
                                var previous = user.preferences.incomingReplyTarget;
                                user.preferences.incomingReplyTarget = sender.uuid();
                                return previous;
                            })
                            .handle((previousIncoming, targetFailure) -> new TargetUpdate(previousOutgoing, previousIncoming, targetFailure)))
                    .thenCompose(update -> {
                        if (update.failure() == null) return CompletableFuture.completedFuture(update);
                        return users.updateVoid(sender.uuid(), user -> {
                            if (target.uuid().equals(user.preferences.outgoingReplyTarget)) {
                                user.preferences.outgoingReplyTarget = update.previousOutgoing();
                            }
                        }).handle((_, rollbackFailure) -> {
                            if (rollbackFailure != null) update.failure().addSuppressed(rollbackFailure);
                            return update;
                        });
                    }).thenCompose(update -> {
                        if (update.failure() != null) {
                            return CompletableFuture.completedFuture(MessageResult.failure("service.messaging.persistence-failed"));
                        }
                        return platform.callOnServerThread(() -> {
                            var senderName = displayNames.plainDisplayName(sender);
                            var targetName = displayNames.plainDisplayName(target);
                            platform.sendMessage(target, renderer.render(
                                    platform.locale(target),
                                    "messaging.private-incoming",
                                    Map.of("sender", displayNames.displayName(sender), "message", message)
                            ));
                            platform.sendMessage(sender, renderer.render(
                                    platform.locale(sender),
                                    "messaging.private-outgoing",
                                    Map.of("target", displayNames.displayName(target), "message", message)
                            ));
                            return new DeliveredNames(senderName, targetName);
                        }).thenCompose(names -> broadcastSpy(
                                names.sender(),
                                names.target(),
                                message,
                                Set.of(sender.uuid(), target.uuid())
                        )).thenApply(_ -> MessageResult.success("service.messaging.sent"));
                    });
        }).exceptionally(_ -> MessageResult.failure("service.messaging.persistence-failed"));
    }

    @Override
    public CompletableFuture<Optional<UUID>> lastReplyTarget(UUID uuid) {
        return users.load(uuid).thenApply(user -> {
            var preferred = user.preferences.replyToLastRecipient
                    ? user.preferences.outgoingReplyTarget
                    : user.preferences.incomingReplyTarget;
            var fallback = user.preferences.replyToLastRecipient
                    ? user.preferences.incomingReplyTarget
                    : user.preferences.outgoingReplyTarget;
            return Optional.ofNullable(preferred != null ? preferred : fallback);
        });
    }

    @Override
    public CompletableFuture<Void> setLastReplyTarget(UUID uuid, UUID target) {
        return users.updateVoid(uuid, user -> user.preferences.incomingReplyTarget = target);
    }

    @Override
    public CompletableFuture<Boolean> ignored(UUID viewer, UUID target) {
        return users.load(viewer).thenApply(user -> user.relations.ignored.contains(target));
    }

    @Override
    public CompletableFuture<Void> setIgnored(UUID viewer, UUID target, boolean ignored) {
        return users.updateVoid(viewer, user -> {
            if (ignored) user.relations.ignored.add(target);
            else user.relations.ignored.remove(target);
        });
    }

    @Override
    public CompletableFuture<Boolean> socialSpy(UUID uuid) {
        return users.load(uuid).thenApply(user -> user.preferences.socialSpy);
    }

    @Override
    public CompletableFuture<Void> setSocialSpy(UUID uuid, boolean enabled) {
        return users.updateVoid(uuid, user -> user.preferences.socialSpy = enabled);
    }

    @Override
    public CompletableFuture<Void> broadcastSpy(
            String sender,
            String target,
            String message,
            Collection<UUID> excluded
    ) {
        var excludedSnapshot = Set.copyOf(excluded);
        return platform.callOnServerThread(() ->
                        platform.onlinePlayers().stream()
                                .filter(player -> !excludedSnapshot.contains(player.uuid()))
                                .filter(player -> permissions.has(
                                        player.nativeHandle(),
                                        "cellulosesz.messaging.socialspy"
                                ))
                                .toList()
                )
                .thenCompose(candidates -> {
                    var checks = candidates.stream()
                            .map(player ->
                                    socialSpy(player.uuid())
                                            .exceptionally(_ -> false)
                                            .thenApply(enabled -> new SpyCandidate(player, enabled))
                            )
                            .toList();
                    return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
                            .thenCompose(_ -> platform.runOnServerThreadAsync(() ->
                                    checks.stream()
                                            .map(check ->
                                                    check.getNow(new SpyCandidate(null, false))
                                            )
                                            .filter(candidate -> candidate.enabled() && candidate.player() != null)
                                            .forEach(candidate -> platform.sendMessage(
                                                    candidate.player(),
                                                    renderer.render(
                                                            platform.locale(candidate.player()),
                                                            "messaging.social-spy",
                                                            Map.of(
                                                                    "sender", sender,
                                                                    "target", target,
                                                                    "message", message
                                                            )
                                                    )
                                            ))
                            ));
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
