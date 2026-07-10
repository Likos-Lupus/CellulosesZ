package top.likoslupus.cellulosesz.api.messaging;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;

public interface PrivateMessageService {

    MessageResult send(
            CellPlayer sender,
            CellPlayer target,
            String message
    );

    Optional<UUID> lastReplyTarget(UUID uuid);

    void setLastReplyTarget(UUID uuid, UUID target);

    boolean ignored(UUID viewer, UUID target);

    void setIgnored(
            UUID viewer,
            UUID target,
            boolean ignored
    );

    boolean socialSpy(UUID uuid);

    void setSocialSpy(UUID uuid, boolean enabled);

}
