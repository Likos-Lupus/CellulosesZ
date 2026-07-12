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
    private final ConcurrentHashMap<UUID, UUID> replyTargets = new ConcurrentHashMap<>();
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
        setLastReplyTarget(sender.uuid(), target.uuid());
        setLastReplyTarget(target.uuid(), sender.uuid());

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
        return Optional.ofNullable(replyTargets.get(uuid));
    }

    @Override
    public void setLastReplyTarget(UUID uuid, UUID target) {
        replyTargets.put(uuid, target);
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
        if (ignored) {
            user.relations.ignored.add(target);
        } else {
            user.relations.ignored.remove(target);
        }
        users.markDirty(viewer);
        users.save(viewer);
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
