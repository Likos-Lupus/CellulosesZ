package top.likoslupus.cellulosesz.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

final class AsyncQueueLifecycleTest {

    @Test
    void sameKeyIsStrictlySerialAndFailureDoesNotPoisonFollowingOperation() {
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
    void acceptedSubmissionIsDrainedAndCloseLinearizationRejectsLaterKeys() {
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
    void stopBeforeSubmitDoesNotCreateAQueue() {
        var queue = new KeyedSerialAsyncQueue<String>(Runnable::run, 16);
        queue.stopAccepting();

        assertTrue(queue.submit(
                                "late",
                                () -> CompletableFuture.completedFuture(null)
                        )
                        .isCompletedExceptionally()
        );
        queue.drain().join();
        assertEquals(0, queue.activeKeys());
    }

}
