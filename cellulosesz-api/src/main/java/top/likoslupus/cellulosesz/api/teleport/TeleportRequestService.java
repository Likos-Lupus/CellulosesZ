package top.likoslupus.cellulosesz.api.teleport;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;

public interface TeleportRequestService {

    TeleportRequest create(
            CellPlayer requester,
            CellPlayer target,
            TeleportRequestType type,
            int timeoutSeconds
    );

    Optional<TeleportRequest> pendingFor(UUID target);

    Optional<TeleportRequest> removeFor(UUID target);

    boolean cancel(UUID requester);

    void clearExpired();

}
