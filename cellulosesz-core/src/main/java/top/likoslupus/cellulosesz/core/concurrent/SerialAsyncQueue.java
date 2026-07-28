package top.likoslupus.cellulosesz.core.concurrent;

import java.util.concurrent.*;
import java.util.function.Supplier;

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
    private boolean closed;

    public SerialAsyncQueue(Executor executor, int maximumPending) {
        this.executor = requireNonNull(executor, "executor");
        if (maximumPending <= 0) {
            throw new IllegalArgumentException("maximumPending must be greater than 0");
        }
        this.maximumPending = maximumPending;
    }

    public <T> CompletableFuture<T> submit(Supplier<? extends CompletionStage<T>> operation) {
        requireNonNull(operation, "operation");
        var result = new CompletableFuture<T>();
        synchronized (lock) {
            if (closed) {
                return CompletableFuture.failedFuture(new IllegalStateException("Queue is closed"));
            }
            if (pending >= maximumPending) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Queue pending limit reached: " + maximumPending
                ));
            }

            pending++;
            var start = tail.handleAsync(
                    (_, _) -> null,
                    executor
            );
            tail = start.thenComposeAsync(_ -> invoke(operation), executor)
                    .handle((value, failure) -> {
                        synchronized (lock) {
                            pending--;
                        }
                        if (failure == null) result.complete(value);
                        else result.completeExceptionally(unwrap(failure));
                        return (Void) null;
                    });
        }
        return result;
    }

    private static <T> CompletionStage<T> invoke(Supplier<? extends CompletionStage<T>> operation) {
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

    public CompletableFuture<Void> drain() {
        synchronized (lock) {
            return tail.handle((_, _) -> (Void) null);
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
        synchronized (lock) {
            closed = true;
            return tail.handle((_, _) -> (Void) null);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
        }
    }

}
