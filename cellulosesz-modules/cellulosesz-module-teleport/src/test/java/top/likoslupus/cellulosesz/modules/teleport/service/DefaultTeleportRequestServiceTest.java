package top.likoslupus.cellulosesz.modules.teleport.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestSelectionResult;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

import static java.util.stream.IntStream.range;

final class DefaultTeleportRequestServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-30T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void differentRequestTypesForSamePairCoexistAndPlayerSelectionIsAmbiguous() {
        var service = new DefaultTeleportRequestService(CLOCK);
        var requester = player("requester");
        var target = player("target");

        var tpa = service.create(
                requester,
                target,
                TeleportRequestType.REQUESTER_TO_TARGET,
                60
        );
        var here = service.create(
                requester,
                target,
                TeleportRequestType.TARGET_TO_REQUESTER,
                60
        );

        assertTrue(tpa.created());
        assertTrue(here.created());
        assertEquals(2, service.pendingFor(target.uuid()).size());
        assertInstanceOf(
                TeleportRequestSelectionResult.Ambiguous.class,
                service.selectIncoming(
                        target.uuid(),
                        Optional.of(requester.uuid()),
                        Optional.empty()
                )
        );
        assertInstanceOf(
                TeleportRequestSelectionResult.Selected.class,
                service.selectIncoming(
                        target.uuid(),
                        Optional.empty(),
                        Optional.of(tpa.request().id())
                )
        );
    }

    private static CellPlayer player(String name) {
        return new CellPlayer(
                UUID.randomUUID(),
                name
        );
    }

    @Test
    void duplicateRequestIsReportedWithoutReplacingStableId() {
        var service = new DefaultTeleportRequestService(CLOCK);
        var requester = player("requester");
        var target = player("target");

        var first = service.create(
                requester,
                target,
                TeleportRequestType.REQUESTER_TO_TARGET,
                60
        );
        var duplicate = service.create(
                requester,
                target,
                TeleportRequestType.REQUESTER_TO_TARGET,
                60
        );

        assertTrue(first.created());
        assertFalse(duplicate.created());
        assertEquals(first.request().id(), duplicate.request().id());
        assertEquals(1, service.pendingFor(target.uuid()).size());
    }

    @Test
    void claimIsAtomicAndReleaseMakesFailureRetryable() throws Exception {
        var service = new DefaultTeleportRequestService(CLOCK);
        var request = service.create(
                player("requester"),
                player("target"),
                TeleportRequestType.REQUESTER_TO_TARGET, 60
        ).request();
        var wins = new AtomicInteger();
        var start = new CountDownLatch(1);

        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            var tasks = range(0, 32)
                    .mapToObj(_ -> executor.submit(() -> {
                        start.await();
                        if (service.claim(request.id()).isPresent()) {
                            wins.incrementAndGet();
                        }
                        return null;
                    }))
                    .toList();
            start.countDown();
            for (var task : tasks) task.get();
        }

        assertEquals(1, wins.get());
        assertTrue(service.release(request.id()));
        assertTrue(service.claim(request.id()).isPresent());
        assertTrue(service.complete(request.id()));
        assertTrue(service.pending(request.id()).isEmpty());
    }

    @Test
    void outgoingPlayerSelectionIsAmbiguousWhenBothTypesExist() {
        var service = new DefaultTeleportRequestService(CLOCK);
        var requester = player("requester");
        var target = player("target");

        service.create(requester, target, TeleportRequestType.REQUESTER_TO_TARGET, 60);
        service.create(requester, target, TeleportRequestType.TARGET_TO_REQUESTER, 60);

        assertInstanceOf(
                TeleportRequestSelectionResult.Ambiguous.class,
                service.selectOutgoing(
                        requester.uuid(),
                        Optional.of(target.uuid()),
                        Optional.empty()
                )
        );
    }

}
