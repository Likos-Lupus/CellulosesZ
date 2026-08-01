package top.likoslupus.cellulosesz.modules.user.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.NameCacheService;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;

import static org.junit.jupiter.api.Assertions.*;

final class JsonUserServiceTest {

    @Test
    void unrelatedPreferenceUpdatePreservesConfirmInventoryClears() {
        var service = new JsonUserService(
                new MemoryStorage(),
                new MemoryNames(),
                Path.of("users"),
                new NoopLogger()
        );
        var uuid = UUID.randomUUID();

        service
                .updateVoid(
                        uuid,
                        user -> user.withPreferences(user
                                .preferences()
                                .withConfirmInventoryClears(false))
                )
                .join();
        service
                .updateVoid(
                        uuid,
                        user -> user.withPreferences(user.preferences().withPayments(false))
                )
                .join();

        var snapshot = service.cached(uuid).orElseThrow();
        assertFalse(snapshot.preferences().confirmInventoryClears());
        assertFalse(snapshot.preferences().payments());
        assertTrue(snapshot.preferences().privateMessages());
        assertTrue(snapshot.preferences().teleportRequests());
        assertTrue(snapshot.preferences().confirmLargePayments());
        assertTrue(snapshot.preferences().powerToolsEnabled());
    }


    @Test
    void concurrentFirstLoadSharesOneStorageOperation() {
        var storage = new MemoryStorage();
        storage.createGate = new CompletableFuture<>();
        var service = new JsonUserService(
                storage,
                new MemoryNames(),
                Path.of("users"),
                new NoopLogger()
        );
        var uuid = UUID.randomUUID();

        var first = service.load(uuid);
        var second = service.load(uuid);

        assertSame(first, second);
        assertEquals(1, storage.createCalls.get());
        storage.createGate.complete(null);
        assertSame(first.join(), second.join());
    }

    @Test
    void storageFailureDoesNotPublishReplacement() {
        var storage = new MemoryStorage();
        var service = new JsonUserService(
                storage,
                new MemoryNames(),
                Path.of("users"),
                new NoopLogger()
        );
        var uuid = UUID.randomUUID();

        service
                .updateVoid(
                        uuid,
                        user -> user.withPreferences(user.preferences().withPayments(false))
                )
                .join();
        storage.failSaves = true;

        assertThrows(
                Exception.class,
                () -> service.updateVoid(
                        uuid,
                        user -> user.withPreferences(user.preferences().withPrivateMessages(false))
                ).join()
        );

        var cached = service.cached(uuid).orElseThrow();
        assertFalse(cached.preferences().payments());
        assertTrue(cached.preferences().privateMessages());
    }

    @Test
    void playTimeOverflowSaturatesWithoutEscapingTheQuitPath() {
        var service = new JsonUserService(
                new MemoryStorage(),
                new MemoryNames(),
                Path.of("users"),
                new NoopLogger()
        );
        var uuid = UUID.randomUUID();
        var startedAt = System.currentTimeMillis() - 1_000L;

        service.updateVoid(
                uuid,
                user -> user.withTimestamps(
                        user.timestamps()
                                .withPlayTimeMillis(Long.MAX_VALUE)
                                .withActiveSessionStartedAt(startedAt)
                )
        ).join();

        service
                .markQuit(new CellPlayer(
                        uuid,
                        "player",
                        new Object()
                ))
                .join();

        var snapshot = service.cached(uuid).orElseThrow();
        assertEquals(Long.MAX_VALUE, snapshot.timestamps().playTimeMillis());
        assertNull(snapshot.timestamps().activeSessionStartedAt());
    }

    @NullMarked
    private static final class MemoryStorage implements StorageService {

        private final Map<Path, Object> values = new ConcurrentHashMap<>();
        private final AtomicInteger createCalls = new AtomicInteger();
        private CompletableFuture<Void> createGate = CompletableFuture.completedFuture(null);
        private boolean failSaves;

        @Override
        public <T> CompletableFuture<T> loadOrDefault(
                Path path,
                Class<T> type,
                Supplier<T> defaults
        ) {
            var value = values.get(path);
            return CompletableFuture.completedFuture(value == null
                    ? defaults.get()
                    : type.cast(value));
        }

        @Override
        public <T> CompletableFuture<T> createIfMissing(
                Path path,
                Class<T> type,
                Supplier<T> defaults
        ) {
            createCalls.incrementAndGet();
            return createGate.thenApply(_ -> type.cast(values.computeIfAbsent(
                    path,
                    _ -> defaults.get()
            )));
        }

        @Override
        public <T> CompletableFuture<Void> save(Path path, T value) {
            if (failSaves) {
                return CompletableFuture.failedFuture(new IllegalStateException("disk failure"));
            }

            values.put(path, value);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> exists(Path path) {
            return CompletableFuture.completedFuture(values.containsKey(path));
        }

        @Override
        public CompletableFuture<Boolean> delete(Path path) {
            return CompletableFuture.completedFuture(values.remove(path) != null);
        }

        @Override
        public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
            return CompletableFuture.completedFuture(List.of());
        }

    }

    @NullMarked
    private static final class MemoryNames implements NameCacheService {

        private final Map<UUID, String> names = new HashMap<>();

        @Override
        public void remember(UUID uuid, String name) {
            names.put(uuid, name);
        }

        @Override
        public Optional<UUID> findUuid(String name) {
            return names.entrySet().stream()
                    .filter(e -> e.getValue().equalsIgnoreCase(name))
                    .map(Map.Entry::getKey)
                    .findFirst();
        }

        @Override
        public Optional<String> findName(UUID uuid) {
            return Optional.ofNullable(names.get(uuid));
        }

        @Override
        public Map<UUID, String> entries() {
            return Map.copyOf(names);
        }

        @Override
        public CompletableFuture<Void> save() {
            return CompletableFuture.completedFuture(null);
        }

    }

    @NullMarked
    private static final class NoopLogger implements CellulosesZLogger {

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable throwable) {
        }

        @Override
        public void info(String message) {
        }

    }

}
