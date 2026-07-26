package top.likoslupus.cellulosesz.modules.teleport.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.IntStream.*;
import static org.junit.jupiter.api.Assertions.*;

final class DefaultTeleportRequestServiceTest {

    @Test
    void differentRequestTypesForSamePairCoexist() {
        var service = new DefaultTeleportRequestService();
        var requester = player("requester");
        var target = player("target");

        var tpa = service.create(requester, target, TeleportRequestType.REQUESTER_TO_TARGET, 60);
        var here = service.create(requester, target, TeleportRequestType.TARGET_TO_REQUESTER, 60);

        assertTrue(tpa.created());
        assertTrue(here.created());
        assertEquals(2, service.pendingFor(target.uuid()).size());
        assertTrue(service.pending(tpa.request().id()).isPresent());
        assertTrue(service.pending(here.request().id()).isPresent());
    }

    private static CellPlayer player(String name) {
        return new CellPlayer(UUID.randomUUID(), name, new Object());
    }

    @Test
    void duplicateRequestIsReportedWithoutReplacingStableId() {
        var service = new DefaultTeleportRequestService();
        var requester = player("requester");
        var target = player("target");

        var first = service.create(requester, target, TeleportRequestType.REQUESTER_TO_TARGET, 60);
        var duplicate = service.create(requester, target, TeleportRequestType.REQUESTER_TO_TARGET, 60);

        assertTrue(first.created());
        assertFalse(duplicate.created());
        assertEquals(first.request().id(), duplicate.request().id());
        assertEquals(1, service.pendingFor(target.uuid()).size());
    }

    @Test
    void claimIsAtomicUnderConcurrency() throws Exception {
        var service = new DefaultTeleportRequestService();
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
                        if (service.claim(request.id()).isPresent()) wins.incrementAndGet();
                        return null;
                    }))
                    .toList();
            start.countDown();
            for (var task : tasks) task.get();
        }

        assertEquals(1, wins.get());
        assertTrue(service.complete(request.id()));
        assertTrue(service.pending(request.id()).isEmpty());
    }

}
