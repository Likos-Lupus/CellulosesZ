package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequest;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultTeleportRequestService implements TeleportRequestService {

    private final ConcurrentHashMap<UUID, TeleportRequest> requestsByTarget = new ConcurrentHashMap<>();

    @Override
    public TeleportRequest create(
            CellPlayer requester,
            CellPlayer target,
            TeleportRequestType type,
            int timeoutSeconds
    ) {
        var now = System.currentTimeMillis();
        var request = new TeleportRequest(
                requester.uuid(),
                target.uuid(),
                type,
                now,
                now + timeoutSeconds * 1000L
        );
        requestsByTarget.put(target.uuid(), request);
        return request;
    }

    @Override
    public Optional<TeleportRequest> pendingFor(UUID target) {
        clearExpired();
        return Optional.ofNullable(requestsByTarget.get(target));
    }

    @Override
    public Optional<TeleportRequest> removeFor(UUID target) {
        clearExpired();
        return Optional.ofNullable(requestsByTarget.remove(target));
    }

    @Override
    public boolean cancel(UUID requester) {
        clearExpired();
        return requestsByTarget.entrySet().removeIf(entry -> entry.getValue().requester().equals(requester));
    }

    @Override
    public void clearExpired() {
        var now = System.currentTimeMillis();
        requestsByTarget.entrySet().removeIf(entry -> entry.getValue().expired(now));
    }

}
