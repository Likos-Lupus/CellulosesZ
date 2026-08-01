package top.likoslupus.cellulosesz.modules.economy.service;

import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.economy.data.WorthDocument;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class JsonWorthService implements WorthService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_MUTATIONS = 4_096;

    private final StorageService storage;
    private final Path path;
    private final Object lock = new Object();
    private final SerialAsyncQueue mutations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_MUTATIONS
    );
    private WorthDocument document;

    public JsonWorthService(
            StorageService storage,
            Path directory
    ) {
        this.storage = storage;
        this.path = directory.resolve("worth.json");
        this.document = new WorthDocument();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage
                .createIfMissing(
                        path,
                        WorthDocument.class,
                        WorthDocument::new
                )
                .thenApply(loaded -> {
                    validate(loaded);
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (lock) {
                        document = loaded;
                    }
                });
    }

    private void validate(WorthDocument candidate) {
        candidate.prices.forEach((item, value) -> {
            if (!normalize(item).equals(item)) {
                throw new IllegalStateException(
                        "Worth item IDs must be normalized namespaced identifiers"
                );
            }

            var amount = new BigDecimal(value);
            if (amount.signum() < 0) {
                throw new IllegalStateException("Worth must not be negative");
            }
        });
    }

    private String normalize(String itemId) {
        var value = itemId.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+|[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid item identifier: " + itemId);
        }

        return value.indexOf(':') < 0
                ? "minecraft:" + value
                : value;
    }

    @Override
    public Optional<BigDecimal> worth(String itemId) {
        synchronized (lock) {
            var value = document.prices.get(normalize(itemId));
            return value == null
                    ? Optional.empty()
                    : Optional.of(new BigDecimal(value));
        }
    }

    @Override
    public CompletableFuture<Void> setWorth(String itemId, BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Worth must not be negative");
        }

        var key = normalize(itemId);
        var encoded = amount.stripTrailingZeros().toPlainString();

        return enqueue(candidate -> {
            candidate.prices.put(key, encoded);
            return true;
        }).thenApply(_ -> null);
    }

    @Override
    public CompletableFuture<Boolean> removeWorth(String itemId) {
        var key = normalize(itemId);
        return enqueue(candidate ->
                candidate.prices.remove(key) != null
        );
    }

    @Override
    public Map<String, BigDecimal> allWorths() {
        synchronized (lock) {
            var result = new LinkedHashMap<String, BigDecimal>();
            document.prices.forEach((item, value) -> result.put(
                    item,
                    new BigDecimal(value)
            ));
            return Map.copyOf(result);
        }
    }

    private <T> CompletableFuture<T> enqueue(Function<WorthDocument, T> mutation) {
        return mutations.submit(() -> {
            WorthDocument candidate;
            T value;
            synchronized (lock) {
                candidate = copy(document);
                value = mutation.apply(candidate);
                validate(candidate);
            }

            return storage
                    .save(path, candidate)
                    .thenApply(_ -> {
                        synchronized (lock) {
                            document = candidate;
                        }

                        return value;
                    });
        });
    }

    private WorthDocument copy(WorthDocument source) {
        var copy = new WorthDocument();
        copy.prices.putAll(source.prices);
        return copy;
    }


    @Override
    public void stopAccepting() {
        mutations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return mutations.drain();
    }

}
