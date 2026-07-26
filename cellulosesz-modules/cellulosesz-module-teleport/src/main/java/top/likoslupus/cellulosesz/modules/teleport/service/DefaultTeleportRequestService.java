package top.likoslupus.cellulosesz.modules.teleport.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequest;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultTeleportRequestService implements TeleportRequestService {

    private static final Comparator<TeleportRequest> NEWEST_FIRST = Comparator
            .comparingLong(TeleportRequest::createdAtMillis)
            .reversed()
            .thenComparing(TeleportRequest::id);

    private final ConcurrentHashMap<UUID, RequestEntry> requestsById = new ConcurrentHashMap<>();
    private final Object mutationLock = new Object();

    @Override
    public TeleportRequest create(
            CellPlayer requester,
            CellPlayer target,
            TeleportRequestType type,
            int timeoutSeconds
    ) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be greater than zero");
        }
        var now = System.currentTimeMillis();
        var request = new TeleportRequest(
                UUID.randomUUID(),
                requester.uuid(),
                target.uuid(),
                type,
                now,
                Math.addExact(now, Math.multiplyExact((long) timeoutSeconds, 1000L))
        );
        synchronized (mutationLock) {
            removeExpiredLocked(now);
            requestsById.entrySet().removeIf(entry -> {
                var existing = entry.getValue();
                return existing.state == RequestState.PENDING
                        && existing.request.requester().equals(requester.uuid())
                        && existing.request.target().equals(target.uuid())
                        && existing.request.type() == type;
            });
            requestsById.put(request.id(), new RequestEntry(request));
        }
        return request;
    }

    @Override
    public List<TeleportRequest> pendingFor(UUID target) {
        synchronized (mutationLock) {
            removeExpiredLocked(System.currentTimeMillis());
            return requestsById.values().stream()
                    .filter(entry -> entry.state == RequestState.PENDING)
                    .map(entry -> entry.request)
                    .filter(request -> request.target().equals(target))
                    .sorted(NEWEST_FIRST)
                    .toList();
        }
    }

    @Override
    public Optional<TeleportRequest> pendingFor(UUID target, UUID requester) {
        return pendingFor(target).stream()
                .filter(request -> request.requester().equals(requester))
                .max(Comparator.comparingLong(TeleportRequest::createdAtMillis)
                        .thenComparing(TeleportRequest::id));
    }

    @Override
    public Optional<TeleportRequest> pending(UUID requestId) {
        synchronized (mutationLock) {
            removeExpiredLocked(System.currentTimeMillis());
            var entry = requestsById.get(requestId);
            return entry == null || entry.state != RequestState.PENDING
                    ? Optional.empty()
                    : Optional.of(entry.request);
        }
    }

    @Override
    public Optional<TeleportRequest> newestFor(UUID target) {
        var pending = pendingFor(target);
        return pending.isEmpty() ? Optional.empty() : Optional.of(pending.getFirst());
    }

    @Override
    public Optional<TeleportRequest> claim(UUID requestId) {
        synchronized (mutationLock) {
            removeExpiredLocked(System.currentTimeMillis());
            var entry = requestsById.get(requestId);
            if (entry == null || entry.state != RequestState.PENDING) return Optional.empty();
            entry.state = RequestState.CONSUMING;
            return Optional.of(entry.request);
        }
    }

    @Override
    public boolean release(UUID requestId) {
        synchronized (mutationLock) {
            var entry = requestsById.get(requestId);
            if (entry == null || entry.state != RequestState.CONSUMING) return false;
            if (entry.request.expired(System.currentTimeMillis())) {
                requestsById.remove(requestId, entry);
                return false;
            }
            entry.state = RequestState.PENDING;
            return true;
        }
    }

    @Override
    public boolean complete(UUID requestId) {
        synchronized (mutationLock) {
            var entry = requestsById.get(requestId);
            return entry != null
                    && entry.state == RequestState.CONSUMING
                    && requestsById.remove(requestId, entry);
        }
    }

    @Override
    public boolean remove(UUID requestId) {
        synchronized (mutationLock) {
            var entry = requestsById.get(requestId);
            return entry != null
                    && entry.state == RequestState.PENDING
                    && requestsById.remove(requestId, entry);
        }
    }

    @Override
    public int cancel(UUID requester, @Nullable UUID target) {
        synchronized (mutationLock) {
            removeExpiredLocked(System.currentTimeMillis());
            int before = requestsById.size();
            requestsById.entrySet().removeIf(entry -> {
                var value = entry.getValue();
                var request = value.request;
                return value.state == RequestState.PENDING
                        && request.requester().equals(requester)
                        && (target == null || request.target().equals(target));
            });
            return before - requestsById.size();
        }
    }

    @Override
    public int clearFor(UUID player) {
        synchronized (mutationLock) {
            int before = requestsById.size();
            requestsById.entrySet().removeIf(entry -> {
                var request = entry.getValue().request;
                return request.requester().equals(player) || request.target().equals(player);
            });
            return before - requestsById.size();
        }
    }

    @Override
    public int clearExpired() {
        synchronized (mutationLock) {
            return removeExpiredLocked(System.currentTimeMillis());
        }
    }

    private int removeExpiredLocked(long now) {
        var expired = new ArrayList<UUID>();
        requestsById.forEach((id, entry) -> {
            if (entry.state == RequestState.PENDING && entry.request.expired(now)) expired.add(id);
        });
        expired.forEach(requestsById::remove);
        return expired.size();
    }

    private enum RequestState {
        PENDING,
        CONSUMING
    }

    private static final class RequestEntry {

        private final TeleportRequest request;
        private RequestState state = RequestState.PENDING;

        private RequestEntry(TeleportRequest request) {
            this.request = request;
        }

    }

}
