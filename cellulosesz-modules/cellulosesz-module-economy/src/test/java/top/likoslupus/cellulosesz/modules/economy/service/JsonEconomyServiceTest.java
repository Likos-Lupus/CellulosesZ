package top.likoslupus.cellulosesz.modules.economy.service;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class JsonEconomyServiceTest {

    @Test
    void failedSaveDoesNotPublishBalance() {
        var storage = new MemoryStorage();
        var service = new JsonEconomyService(
                storage,
                new EconomyConfig(),
                Path.of("economy"),
                new NoopLogger()
        );
        var player = UUID.randomUUID();
        storage.failSaves = true;

        var result = service.deposit(
                player,
                new BigDecimal("10.00"),
                TransactionCause.system("test")
        ).join();
        assertFalse(result.success());
        assertEquals("service.economy.persistence-failed", result.message().key());
        assertEquals(new BigDecimal("0.00"), service.balance(player));
    }

    @Test
    void multiRecipientTransferCommitsAsOneDocument() {
        var storage = new MemoryStorage();
        var service = new JsonEconomyService(
                storage,
                new EconomyConfig(),
                Path.of("economy"),
                new NoopLogger()
        );
        var payer = UUID.randomUUID();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        service.deposit(
                payer,
                new BigDecimal("30.00"),
                TransactionCause.system("seed")
        ).join();
        var savesBefore = storage.saves;

        var result = service.transferMany(
                payer,
                List.of(first, second),
                new BigDecimal("10.00"),
                TransactionCause.system("test")
        ).join();

        assertTrue(result.success());
        assertEquals(new BigDecimal("10.00"), service.balance(payer));
        assertEquals(new BigDecimal("10.00"), service.balance(first));
        assertEquals(new BigDecimal("10.00"), service.balance(second));
        assertEquals(savesBefore + 1, storage.saves);
    }

    @NullMarked
    private static final class MemoryStorage implements StorageService {

        private @Nullable Object document;
        private boolean failSaves;
        private int saves;

        @Override
        public <T> CompletableFuture<T> loadOrDefault(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            if (document == null) return CompletableFuture.completedFuture(defaultSupplier.get());
            return CompletableFuture.completedFuture(type.cast(document));
        }

        @Override
        public <T> CompletableFuture<T> createIfMissing(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            if (document == null) document = defaultSupplier.get();
            return CompletableFuture.completedFuture(type.cast(document));
        }

        @Override
        public <T> CompletableFuture<Void> save(Path path, T value) {
            if (failSaves) return CompletableFuture.failedFuture(new IllegalStateException("disk failure"));
            document = value;
            saves++;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> exists(Path path) {
            return CompletableFuture.completedFuture(document != null);
        }

        @Override
        public CompletableFuture<Boolean> delete(Path path) {
            document = null;
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
            return CompletableFuture.completedFuture(List.of());
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
