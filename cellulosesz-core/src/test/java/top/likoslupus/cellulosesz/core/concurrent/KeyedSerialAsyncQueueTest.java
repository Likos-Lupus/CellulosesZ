package top.likoslupus.cellulosesz.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

final class KeyedSerialAsyncQueueTest {

    @Test
    void serializesPerKeyAndCleansIdleKeys() {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var queue = new KeyedSerialAsyncQueue<String>(executor, 8);
            var order = new ArrayList<Integer>();
            var gate = new CompletableFuture<Void>();
            var first = queue.submit("a", () -> gate.thenApply(unused -> {
                synchronized (order) { order.add(1); }
                return 1;
            }));
            var second = queue.submit("a", () -> {
                synchronized (order) { order.add(2); }
                return CompletableFuture.completedFuture(2);
            });
            var other = queue.submit("b", () -> CompletableFuture.completedFuture(3));
            assertEquals(3, other.join());
            gate.complete(null);
            assertEquals(1, first.join());
            assertEquals(2, second.join());
            assertEquals(List.of(1, 2), order);
            queue.drain().join();
            assertEquals(0, queue.activeKeys());
        }
    }
}
