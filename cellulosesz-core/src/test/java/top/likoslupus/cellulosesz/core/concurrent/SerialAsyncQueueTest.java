package top.likoslupus.cellulosesz.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

final class SerialAsyncQueueTest {

    @Test
    void preservesOrderAndContinuesAfterFailure() {
        try (var executor = Executors.newSingleThreadExecutor()) {
            var queue = new SerialAsyncQueue(executor, 8);
            var order = new ArrayList<Integer>();
            var first = queue.submit(() -> {
                order.add(1);
                return CompletableFuture.failedFuture(new IllegalStateException("expected"));
            });
            var second = queue.submit(() -> {
                order.add(2);
                return CompletableFuture.completedFuture("ok");
            });
            assertThrows(Exception.class, first::join);
            assertEquals("ok", second.join());
            assertEquals(List.of(1, 2), order);
            queue.closeAndDrain().join();
            assertThrows(Exception.class,
                    () -> queue.submit(() -> CompletableFuture.completedFuture("late")).join());
        }
    }

    @Test
    void appliesBackpressure() {
        var gate = new CompletableFuture<String>();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var queue = new SerialAsyncQueue(executor, 1);
            var accepted = queue.submit(() -> gate);
            assertThrows(Exception.class,
                    () -> queue.submit(() -> CompletableFuture.completedFuture("overflow")).join());
            gate.complete("done");
            assertEquals("done", accepted.join());
        }
    }
}
