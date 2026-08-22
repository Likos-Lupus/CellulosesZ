package top.likoslupus.cellulosesz.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class AsyncQueueLifecycleTest {

    @Test
    void submit_sameKey_serializesAndContinuesAfterFailure() {
        var queue = new KeyedSerialAsyncQueue<String>(Runnable::run, 16);
        var firstGate = new CompletableFuture<Void>();
        var order = new ArrayList<String>();

        var first = queue.submit(
                "player", () -> {
                    order.add("first-start");
                    return firstGate.thenApply(_ -> {
                        order.add("first-end");
                        throw new IllegalStateException("expected");
                    });
                }
        );
        var second = queue.submit(
                "player", () -> {
                    order.add("second");
                    return CompletableFuture.completedFuture(2);
                }
        );
        var otherKey = queue.submit(
                "other", () -> {
                    order.add("other");
                    return CompletableFuture.completedFuture(3);
                }
        );

        assertEquals(3, otherKey.join());
        assertEquals(List.of("first-start", "other"), order);
        firstGate.complete(null);

        assertThrows(RuntimeException.class, first::join);
        assertEquals(2, second.join());
        assertEquals(List.of("first-start", "other", "first-end", "second"), order);
    }

    @Test
    void submit_concurrentSubmissions_sameKey_neverOverlapAndExecuteInOrder() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            var queue = new KeyedSerialAsyncQueue<String>(executor, 256);
            var activeCount = new AtomicInteger();
            var overlapDetected = new AtomicBoolean(false);
            var completedCount = new AtomicInteger();
            var executionOrder = Collections.synchronizedList(new ArrayList<Integer>());

            var taskCount = 50;
            var submitters = Executors.newFixedThreadPool(8);
            var readyLatch = new CountDownLatch(taskCount);
            var startLatch = new CountDownLatch(1);
            var futures = new ArrayList<CompletableFuture<Integer>>();

            for (var i = 0; i < taskCount; i++) {
                final var taskId = i;
                var future = new CompletableFuture<Integer>();
                futures.add(future);

                submitters.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                    }

                    queue.submit(
                            "shared-key", () -> {
                                var current = activeCount.incrementAndGet();
                                if (current > 1) {
                                    overlapDetected.set(true);
                                }

                                executionOrder.add(taskId);
                                activeCount.decrementAndGet();
                                completedCount.incrementAndGet();
                                return CompletableFuture.completedFuture(taskId);
                            }
                    ).whenComplete((res, err) -> {
                        if (err != null) {
                            future.completeExceptionally(err);
                        } else {
                            future.complete(res);
                        }
                    });
                });
            }

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .get(10, TimeUnit.SECONDS);
            submitters.shutdown();
            submitters.awaitTermination(5, TimeUnit.SECONDS);

            assertFalse(overlapDetected.get(), "Same-key tasks must never execute concurrently");
            assertEquals(taskCount, completedCount.get());
            assertEquals(taskCount, executionOrder.size());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void submit_independentKeys_executeConcurrently() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            var queue = new KeyedSerialAsyncQueue<String>(executor, 16);
            var keyAGate = new CompletableFuture<Void>();
            var keyAStarted = new CountDownLatch(1);

            var futureA = queue.submit(
                    "keyA", () -> {
                        keyAStarted.countDown();
                        return keyAGate.thenApply(_ -> "resultA");
                    }
            );

            keyAStarted.await(5, TimeUnit.SECONDS);

            // keyB must be able to complete while keyA is blocked on keyAGate
            var futureB = queue.submit("keyB", () -> CompletableFuture.completedFuture("resultB"));
            assertEquals("resultB", futureB.get(5, TimeUnit.SECONDS));
            assertFalse(futureA.isDone());

            keyAGate.complete(null);
            assertEquals("resultA", futureA.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void close_afterAcceptedSubmission_drainsAndRejectsLaterKeys() {
        var queue = new KeyedSerialAsyncQueue<String>(Runnable::run, 16);
        var gate = new CompletableFuture<Void>();
        var accepted = queue.submit("accepted", () -> gate);

        var close = queue.closeAndDrain();
        assertFalse(close.isDone());
        assertTrue(queue.submit(
                                "late",
                                () -> CompletableFuture.completedFuture(null)
                        )
                        .isCompletedExceptionally()
        );
        assertEquals(1, queue.activeKeys());

        gate.complete(null);
        accepted.join();
        close.join();
        assertEquals(0, queue.activeKeys());
    }

    @Test
    void submit_afterStopAccepting_doesNotCreateQueue() {
        var queue = new KeyedSerialAsyncQueue<String>(Runnable::run, 16);
        queue.stopAccepting();

        assertTrue(queue.submit(
                "late",
                () -> CompletableFuture.completedFuture(null)
        ).isCompletedExceptionally());
        queue.drain().join();
        assertEquals(0, queue.activeKeys());
    }

}
