package top.likoslupus.cellulosesz.core.concurrent;

import java.util.concurrent.*;
import java.util.function.Supplier;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

/**
 * Executes asynchronous operations in submission order without blocking the queue thread.
 */
public final class SerialAsyncQueue implements AutoCloseable {

    private final Executor executor;
    private final int maximumPending;
    private final Object lock = new Object();
    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
    private int pending;
    private boolean accepting = true;

    public SerialAsyncQueue(Executor executor, int maximumPending) {
        this.executor = requireNonNull(executor, "executor");
        this.maximumPending = requirePositive(maximumPending, "maximumPending");
    }

    public <T> CompletableFuture<T> submit(
            Supplier<? extends CompletionStage<T>> operation
    ) {
        requireNonNull(operation, "operation");
        final CompletableFuture<Void> predecessor;
        final var start = new CompletableFuture<Void>();
        final var result = new CompletableFuture<T>();

        synchronized (lock) {
            if (!accepting) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Queue is not accepting operations"
                ));
            }

            if (pending >= maximumPending) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Queue pending limit reached: " + maximumPending
                ));
            }

            pending++;
            predecessor = tail;
            var operationFuture = start.thenComposeAsync(
                    _ -> invoke(operation),
                    executor
            );

            tail = operationFuture
                    .handle((value, failure) -> {
                        synchronized (lock) {
                            pending--;
                        }

                        if (failure == null) {
                            result.complete(value);
                        } else {
                            result.completeExceptionally(unwrap(failure));
                        }

                        return (Void) null;
                    });
        }

        predecessor.whenComplete((_, _) -> start.complete(null));
        return result;
    }

    private static <T> CompletionStage<T> invoke(
            Supplier<? extends CompletionStage<T>> operation
    ) {
        try {
            return requireNonNull(operation.get(), "operation result");
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private static Throwable unwrap(Throwable failure) {
        var current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null
        ) {
            current = current.getCause();
        }
        return current;
    }

    public boolean accepting() {
        synchronized (lock) {
            return accepting;
        }
    }

    public boolean idle() {
        return pending() == 0;
    }

    public int pending() {
        synchronized (lock) {
            return pending;
        }
    }

    public CompletableFuture<Void> closeAndDrain() {
        stopAccepting();
        return drain();
    }

    public void stopAccepting() {
        synchronized (lock) {
            accepting = false;
        }
    }

    public CompletableFuture<Void> drain() {
        synchronized (lock) {
            return tail.thenApply(_ -> null);
        }
    }

    @Override
    public void close() {
        stopAccepting();
    }

}
