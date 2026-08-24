package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.modules.teleport.domain.TeleportRequest;
import top.likoslupus.cellulosesz.modules.teleport.domain.TeleportRequestCreateResult;
import top.likoslupus.cellulosesz.modules.teleport.domain.TeleportRequestSelectionResult;
import top.likoslupus.cellulosesz.modules.teleport.domain.TeleportRequestType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeleportRequestService {

    TeleportRequestCreateResult create(
            CellPlayer requester,
            CellPlayer target,
            TeleportRequestType type,
            int timeoutSeconds
    );

    List<TeleportRequest> pendingFor(UUID target);

    List<TeleportRequest> outgoingFor(UUID requester);

    Optional<TeleportRequest> pending(UUID requestId);

    TeleportRequestSelectionResult selectIncoming(
            UUID target,
            Optional<UUID> requester,
            Optional<UUID> requestId
    );

    TeleportRequestSelectionResult selectOutgoing(
            UUID requester,
            Optional<UUID> target,
            Optional<UUID> requestId
    );

    Optional<TeleportRequest> claim(UUID requestId);

    boolean release(UUID requestId);

    boolean complete(UUID requestId);

    boolean remove(UUID requestId);

    int clearFor(UUID player);

    List<TeleportRequest> clearExpired();

}
