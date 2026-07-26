package top.likoslupus.cellulosesz.modules.teleport.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultTeleportRequestServiceTest {

    @Test
    void differentRequestTypesForSamePairCoexist() {
        var service = new DefaultTeleportRequestService();
        var requester = player("requester");
        var target = player("target");

        var tpa = service.create(requester, target, TeleportRequestType.REQUESTER_TO_TARGET, 60);
        var here = service.create(requester, target, TeleportRequestType.TARGET_TO_REQUESTER, 60);

        assertEquals(2, service.pendingFor(target.uuid()).size());
        assertTrue(service.pending(tpa.id()).isPresent());
        assertTrue(service.pending(here.id()).isPresent());
    }

    private static CellPlayer player(String name) {
        return new CellPlayer(UUID.randomUUID(), name, new Object());
    }

    @Test
    void claimIsAtomicUnderConcurrency() throws Exception {
        var service = new DefaultTeleportRequestService();
        var request = service.create(player("requester"), player("target"),
                TeleportRequestType.REQUESTER_TO_TARGET, 60);
        var wins = new AtomicInteger();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            var tasks = java.util.stream.IntStream.range(0, 32)
                    .mapToObj(index -> executor.submit(() -> {
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
