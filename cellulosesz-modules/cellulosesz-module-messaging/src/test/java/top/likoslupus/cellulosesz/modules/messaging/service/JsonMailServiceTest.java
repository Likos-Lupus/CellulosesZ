package top.likoslupus.cellulosesz.modules.messaging.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.messaging.MailMessage;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class JsonMailServiceTest {

    @Test
    void failedSaveRollsBackMailbox() {
        var storage = new MemoryStorage();
        var service = new JsonMailService(storage, new MessagingConfig(), Path.of("mail.json"));
        var recipient = UUID.randomUUID();
        storage.failSaves = true;

        assertThrows(Exception.class, () -> service.send(message(recipient, null)).join());
        storage.failSaves = false;
        assertTrue(service.inbox(recipient).join().isEmpty());
    }

    private static MailMessage message(UUID recipient, Long expiresAt) {
        var now = System.currentTimeMillis();
        return new MailMessage(UUID.randomUUID(), null, "Console", recipient, "body", now, expiresAt, false);
    }

    @Test
    void sendAllAndExpiryArePersisted() {
        var storage = new MemoryStorage();
        var service = new JsonMailService(storage, new MessagingConfig(), Path.of("mail.json"));
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var now = System.currentTimeMillis();

        assertEquals(2, service.sendAll(List.of(first, second), recipient ->
                message(recipient, now + 1000L)).join());
        assertEquals(1, service.unreadCount(first).join());
        assertEquals(2, service.purgeExpired(now + 2000L).join());
        assertEquals(0, service.unreadCount(first).join());
    }

    private static final class MemoryStorage implements StorageService {

        private Object document;
        private boolean failSaves;

        @Override
        public <T> CompletableFuture<T> load(Path path, Class<T> type, Supplier<T> defaultSupplier) {
            if (document == null) document = defaultSupplier.get();
            return CompletableFuture.completedFuture(type.cast(document));
        }

        @Override
        public <T> CompletableFuture<Void> save(Path path, T value) {
            if (failSaves) return CompletableFuture.failedFuture(new IllegalStateException("disk failure"));
            document = value;
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

}
