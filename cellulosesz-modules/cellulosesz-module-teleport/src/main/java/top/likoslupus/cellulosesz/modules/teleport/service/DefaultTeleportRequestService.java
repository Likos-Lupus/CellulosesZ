package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.*;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;

import static java.util.Objects.requireNonNull;

public final class DefaultTeleportRequestService implements TeleportRequestService {

    private static final Comparator<TeleportRequest> OLDEST_FIRST = Comparator
            .comparingLong(TeleportRequest::createdAtMillis)
            .thenComparing(TeleportRequest::id);

    private final ConcurrentHashMap<UUID, RequestEntry> requestsById = new ConcurrentHashMap<>();
    private final Object mutationLock = new Object();
    private final Clock clock;

    public DefaultTeleportRequestService(Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    @Override
    public TeleportRequestCreateResult create(
            CellPlayer requester,
            CellPlayer target,
            TeleportRequestType type,
            int timeoutSeconds
    ) {
        requireNonNull(requester, "requester");
        requireNonNull(target, "target");
        requireNonNull(type, "type");
        requirePositive(timeoutSeconds, "timeoutSeconds");

        var now = clock.millis();
        final long expires;
        try {
            expires = Math.addExact(
                    now,
                    Math.multiplyExact(timeoutSeconds, 1000L)
            );
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("request expiration overflows", failure);
        }

        synchronized (mutationLock) {
            removeExpiredLocked(now);
            var existing = pendingEntries().stream()
                    .map(entry -> entry.request)
                    .filter(candidate -> candidate.requester().equals(requester.uuid()))
                    .filter(candidate -> candidate.target().equals(target.uuid()))
                    .filter(candidate -> candidate.type() == type).findFirst();

            if (existing.isPresent()) {
                return new TeleportRequestCreateResult(
                        TeleportRequestCreateStatus.ALREADY_PENDING,
                        existing.orElseThrow()
                );
            }

            var request = new TeleportRequest(
                    UUID.randomUUID(),
                    requester.uuid(),
                    target.uuid(),
                    type,
                    now,
                    expires
            );
            requestsById.put(request.id(), new RequestEntry(request));

            return new TeleportRequestCreateResult(TeleportRequestCreateStatus.CREATED, request);
        }
    }

    @Override
    public List<TeleportRequest> pendingFor(UUID target) {
        synchronized (mutationLock) {
            removeExpiredLocked(clock.millis());
            return pendingEntries().stream()
                    .map(entry -> entry.request)
                    .filter(request -> request.target().equals(target))
                    .sorted(OLDEST_FIRST)
                    .toList();
        }
    }

    @Override
    public List<TeleportRequest> outgoingFor(UUID requester) {
        synchronized (mutationLock) {
            removeExpiredLocked(clock.millis());
            return pendingEntries().stream()
                    .map(entry -> entry.request)
                    .filter(request -> request.requester().equals(requester))
                    .sorted(OLDEST_FIRST)
                    .toList();
        }
    }

    @Override
    public Optional<TeleportRequest> pending(UUID requestId) {
        synchronized (mutationLock) {
            removeExpiredLocked(clock.millis());
            var entry = requestsById.get(requestId);
            return entry == null || entry.state != RequestState.PENDING
                    ? Optional.empty()
                    : Optional.of(entry.request);
        }
    }

    @Override
    public TeleportRequestSelectionResult selectIncoming(
            UUID target,
            Optional<UUID> requester,
            Optional<UUID> requestId
    ) {
        requireNonNull(target, "target");
        return select(pendingFor(target).stream()
                .filter(request -> requester.isEmpty()
                        || request.requester().equals(requester.orElseThrow())
                )
                .filter(request -> requestId.isEmpty()
                        || request.id().equals(requestId.orElseThrow())
                ).toList()
        );
    }

    @Override
    public TeleportRequestSelectionResult selectOutgoing(
            UUID requester,
            Optional<UUID> target,
            Optional<UUID> requestId
    ) {
        requireNonNull(requester, "requester");
        return select(outgoingFor(requester).stream()
                .filter(request -> target.isEmpty()
                        || request.target().equals(target.orElseThrow())
                )
                .filter(request -> requestId.isEmpty()
                        || request.id().equals(requestId.orElseThrow())
                ).toList()
        );
    }

    @Override
    public Optional<TeleportRequest> claim(UUID requestId) {
        synchronized (mutationLock) {
            removeExpiredLocked(clock.millis());

            var entry = requestsById.get(requestId);
            if (entry == null || entry.state != RequestState.PENDING) {
                return Optional.empty();
            }

            entry.state = RequestState.CONSUMING;
            return Optional.of(entry.request);
        }
    }

    @Override
    public boolean release(UUID requestId) {
        synchronized (mutationLock) {
            var entry = requestsById.get(requestId);

            if (entry == null || entry.state != RequestState.CONSUMING) {
                return false;
            }

            if (entry.request.expired(clock.millis())) {
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
    public int clearFor(UUID player) {
        synchronized (mutationLock) {
            var before = requestsById.size();
            requestsById.entrySet()
                    .removeIf(entry -> {
                        var request = entry.getValue().request;
                        return request.requester().equals(player)
                                || request.target().equals(player);
                    });
            return before - requestsById.size();
        }
    }

    @Override
    public List<TeleportRequest> clearExpired() {
        synchronized (mutationLock) {
            return removeExpiredLocked(clock.millis());
        }
    }

    private static TeleportRequestSelectionResult select(List<TeleportRequest> matches) {
        return switch (matches.size()) {
            case 0 -> new TeleportRequestSelectionResult.None();
            case 1 -> new TeleportRequestSelectionResult.Selected(matches.getFirst());
            default -> new TeleportRequestSelectionResult.Ambiguous(matches);
        };
    }

    private List<TeleportRequest> removeExpiredLocked(long now) {
        var expired = requestsById.values().stream()
                .filter(entry -> entry.state == RequestState.PENDING
                        && entry.request.expired(now)
                )
                .map(entry -> entry.request)
                .sorted(OLDEST_FIRST)
                .toList();
        expired.forEach(request -> requestsById.remove(request.id()));
        return expired;
    }

    private List<RequestEntry> pendingEntries() {
        return requestsById.values().stream()
                .filter(entry -> entry.state == RequestState.PENDING)
                .toList();
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
