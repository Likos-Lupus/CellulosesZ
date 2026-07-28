package top.likoslupus.cellulosesz.core.concurrent;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Serializes operations per key while allowing different keys to progress independently.
 */
public final class KeyedSerialAsyncQueue<K> implements AutoCloseable {

    private final Executor executor;
    private final int maximumPendingPerKey;
    private final ConcurrentHashMap<K, SerialAsyncQueue> queues = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public KeyedSerialAsyncQueue(Executor executor, int maximumPendingPerKey) {
        this.executor = requireNonNull(executor, "executor");
        if (maximumPendingPerKey <= 0) {
            throw new IllegalArgumentException("maximumPendingPerKey must be greater than 0");
        }
        this.maximumPendingPerKey = maximumPendingPerKey;
    }

    public <T> CompletableFuture<T> submit(
            K key,
            Supplier<? extends CompletionStage<T>> operation
    ) {
        requireNonNull(key, "key");
        requireNonNull(operation, "operation");

        if (closed.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Queue is closed"));
        }
        var queue = queues.computeIfAbsent(
                key,
                _ -> new SerialAsyncQueue(executor, maximumPendingPerKey)
        );
        var result = queue.submit(operation);
        result.whenComplete((_, _) -> {
            if (queue.idle()) queues.remove(key, queue);
        });
        return result;
    }

    public CompletableFuture<Void> drain() {
        var drains = new ArrayList<CompletableFuture<Void>>();
        queues.values()
                .forEach(queue -> drains.add(queue.drain()));
        return CompletableFuture.allOf(drains.toArray(CompletableFuture[]::new));
    }

    public int activeKeys() {
        return queues.size();
    }

    public CompletableFuture<Void> closeAndDrain() {
        closed.set(true);
        var drains = new ArrayList<CompletableFuture<Void>>();
        queues.values()
                .forEach(queue -> drains.add(queue.closeAndDrain()));
        return CompletableFuture.allOf(drains.toArray(CompletableFuture[]::new))
                .whenComplete((_, _) -> queues.clear());
    }

    @Override
    public void close() {
        closed.set(true);
        queues.values()
                .forEach(SerialAsyncQueue::close);
    }

}
