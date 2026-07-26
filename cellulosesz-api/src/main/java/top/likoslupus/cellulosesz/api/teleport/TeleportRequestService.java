package top.likoslupus.cellulosesz.api.teleport;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeleportRequestService {

    TeleportRequest create(
            CellPlayer requester,
            CellPlayer target,
            TeleportRequestType type,
            int timeoutSeconds
    );

    List<TeleportRequest> pendingFor(UUID target);

    Optional<TeleportRequest> pendingFor(UUID target, UUID requester);

    Optional<TeleportRequest> pending(UUID requestId);

    Optional<TeleportRequest> newestFor(UUID target);

    /**
     * Atomically changes a pending request into the consuming state.
     */
    Optional<TeleportRequest> claim(UUID requestId);

    /**
     * Returns a consuming request to pending after a failed teleport.
     */
    boolean release(UUID requestId);

    /**
     * Removes a request after a successful consume.
     */
    boolean complete(UUID requestId);

    boolean remove(UUID requestId);

    int cancel(UUID requester, @Nullable UUID target);

    int clearFor(UUID player);

    int clearExpired();

}
