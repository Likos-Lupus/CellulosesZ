package top.likoslupus.cellulosesz.core.concurrent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

/**
 * Serializes operations per key while allowing different keys to progress independently.
 */
public final class KeyedSerialAsyncQueue<K> implements AutoCloseable {

    private final Executor executor;
    private final int maximumPendingPerKey;
    private final Object lifecycleLock = new Object();
    private final Map<K, SerialAsyncQueue> queues = new LinkedHashMap<>();
    private boolean accepting = true;
    private @Nullable CompletableFuture<Void> closeFuture;

    public KeyedSerialAsyncQueue(
            Executor executor,
            int maximumPendingPerKey
    ) {
        this.executor = requireNonNull(executor, "executor");
        this.maximumPendingPerKey = requirePositive(maximumPendingPerKey, "maximumPendingPerKey");
    }

    public <T> CompletableFuture<T> submit(
            K key,
            Supplier<? extends CompletionStage<T>> operation
    ) {
        requireNonNull(key, "key");
        requireNonNull(operation, "operation");

        final var accepted = new CompletableFuture<Void>();
        final CompletableFuture<T> result;
        synchronized (lifecycleLock) {
            if (!accepting) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Queue is not accepting operations"
                ));
            }

            var queue = queues.computeIfAbsent(
                    key,
                    _ -> new SerialAsyncQueue(executor, maximumPendingPerKey)
            );
            result = queue.submit(() -> accepted.thenCompose(_ -> invoke(operation)));
        }

        accepted.complete(null);
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

    public int activeKeys() {
        synchronized (lifecycleLock) {
            return queues.size();
        }
    }

    public CompletableFuture<Void> closeAndDrain() {
        stopAccepting();
        return drain();
    }

    public void stopAccepting() {
        synchronized (lifecycleLock) {
            if (!accepting) {
                return;
            }

            accepting = false;
            queues.values()
                    .forEach(SerialAsyncQueue::stopAccepting);
        }
    }

    public CompletableFuture<Void> drain() {
        synchronized (lifecycleLock) {
            var drains = new ArrayList<CompletableFuture<Void>>(queues.size());
            queues.values()
                    .forEach(queue -> drains.add(queue.drain()));
            var snapshot = CompletableFuture.allOf(
                    drains.toArray(CompletableFuture[]::new)
            );
            if (accepting) {
                return snapshot;
            }

            if (closeFuture == null) {
                closeFuture = snapshot
                        .whenComplete((_, _) -> {
                            synchronized (lifecycleLock) {
                                queues.clear();
                            }
                        });
            }

            return closeFuture;
        }
    }

    @Override
    public void close() {
        stopAccepting();
    }

}
