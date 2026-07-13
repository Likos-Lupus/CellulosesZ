package top.likoslupus.cellulosesz.modules.messaging.service;

import top.likoslupus.cellulosesz.api.messaging.MessageResult;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultPrivateMessageService implements PrivateMessageService {

    private final PlatformService platform;
    private final UserService users;
    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;
    private final ConcurrentHashMap<UUID, UUID> incomingReplyTargets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> outgoingReplyTargets = new ConcurrentHashMap<>();
    private final Set<UUID> socialSpy = ConcurrentHashMap.newKeySet();

    public DefaultPrivateMessageService(
            PlatformService platform,
            UserService users,
            DisplayNameService displayNames,
            MessageRenderer renderer
    ) {
        this.platform = platform;
        this.users = users;
        this.displayNames = displayNames;
        this.renderer = renderer;
    }

    @Override
    public MessageResult send(
            CellPlayer sender,
            CellPlayer target,
            String message
    ) {
        if (ignored(target.uuid(), sender.uuid())) {
            return MessageResult.failure("service.messaging.ignored");
        }

        var targetUser = users.load(target.uuid()).join();
        if (!targetUser.preferences.privateMessages) {
            return MessageResult.failure("service.messaging.private-messages-disabled");
        }

        platform.sendMessage(target, renderer.render(
                platform.locale(target),
                "messaging.private-incoming",
                Map.of(
                        "sender", displayNames.displayName(sender),
                        "message", message
                )
        ));
        platform.sendMessage(sender, renderer.render(
                platform.locale(sender),
                "messaging.private-outgoing",
                Map.of(
                        "target", displayNames.displayName(target),
                        "message", message
                )
        ));

        outgoingReplyTargets.put(sender.uuid(), target.uuid());
        incomingReplyTargets.put(target.uuid(), sender.uuid());

        platform.onlinePlayers().stream()
                .filter(player -> !player.uuid().equals(sender.uuid())
                        && !player.uuid().equals(target.uuid())
                )
                .filter(player -> socialSpy(player.uuid()))
                .forEach(player -> platform.sendMessage(player, renderer.render(
                        platform.locale(player),
                        "messaging.social-spy",
                        Map.of(
                                "sender", displayNames.displayName(sender),
                                "target", displayNames.displayName(target),
                                "message", message
                        )
                )));
        return MessageResult.success("service.messaging.sent");
    }

    @Override
    public Optional<UUID> lastReplyTarget(UUID uuid) {
        var replyToRecipient = users.load(uuid).join().preferences.replyToLastRecipient;
        var preferred = replyToRecipient
                ? outgoingReplyTargets.get(uuid)
                : incomingReplyTargets.get(uuid);
        var fallback = replyToRecipient
                ? incomingReplyTargets.get(uuid)
                : outgoingReplyTargets.get(uuid);
        return Optional.ofNullable(preferred != null ? preferred : fallback);
    }

    @Override
    public void setLastReplyTarget(UUID uuid, UUID target) {
        incomingReplyTargets.put(uuid, target);
    }

    @Override
    public boolean ignored(UUID viewer, UUID target) {
        return users.load(viewer).join().relations.ignored.contains(target);
    }

    @Override
    public void setIgnored(
            UUID viewer,
            UUID target,
            boolean ignored
    ) {
        var user = users.load(viewer).join();
        var previouslyIgnored = user.relations.ignored.contains(target);
        if (ignored) {
            user.relations.ignored.add(target);
        } else {
            user.relations.ignored.remove(target);
        }
        users.markDirty(viewer);
        try {
            users.save(viewer).join();
        } catch (RuntimeException exception) {
            if (previouslyIgnored) {
                user.relations.ignored.add(target);
            } else {
                user.relations.ignored.remove(target);
            }
            users.markDirty(viewer);
            throw exception;
        }
    }

    @Override
    public boolean socialSpy(UUID uuid) {
        return socialSpy.contains(uuid);
    }

    @Override
    public void setSocialSpy(UUID uuid, boolean enabled) {
        if (enabled) {
            socialSpy.add(uuid);
        } else {
            socialSpy.remove(uuid);
        }
    }

}
