package top.likoslupus.cellulosesz.modules.messaging.service;

import top.likoslupus.cellulosesz.api.messaging.MessageResult;
import top.likoslupus.cellulosesz.api.messaging.PrivateMessageService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultPrivateMessageService implements PrivateMessageService {

    private final PlatformService platform;
    private final UserService users;
    private final ConcurrentHashMap<UUID, UUID> replyTargets = new ConcurrentHashMap<>();
    private final Set<UUID> socialSpy = ConcurrentHashMap.newKeySet();

    public DefaultPrivateMessageService(
            PlatformService platform,
            UserService users
    ) {
        this.platform = platform;
        this.users = users;
    }

    @Override
    public MessageResult send(
            CellPlayer sender,
            CellPlayer target,
            String message
    ) {
        if (ignored(target.uuid(), sender.uuid())) {
            return MessageResult.failure("该玩家忽略了你。 ");
        }

        var targetUser = users.load(target.uuid()).join();
        if (!targetUser.preferences.privateMessages) {
            return MessageResult.failure("该玩家当前不接收私聊。 ");
        }

        platform.sendMessage(target, "[私聊] " + sender.name() + " -> 你: " + message);
        platform.sendMessage(sender, "[私聊] 你 -> " + target.name() + ": " + message);
        setLastReplyTarget(sender.uuid(), target.uuid());
        setLastReplyTarget(target.uuid(), sender.uuid());

        platform.onlinePlayers().stream()
                .filter(player -> !player.uuid().equals(sender.uuid()) && !player.uuid().equals(target.uuid()))
                .filter(player -> socialSpy(player.uuid()))
                .forEach(player -> platform.sendMessage(player, "[SocialSpy] %s -> %s: %s".formatted(sender.name(), target.name(), message)));
        return MessageResult.success("消息已发送。 ");
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
