package top.likoslupus.cellulosesz.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class JacksonStorageServiceTest {

    @TempDir Path root;

    @Test
    void rejectsTraversalAndAbsolutePaths() {
        try (var executor = Executors.newSingleThreadExecutor()) {
            var storage = new JacksonStorageService(root, executor, new NoopLogger());
            assertThrows(IllegalArgumentException.class,
                    () -> storage.save(Path.of("..", "escape.json"), new Document()).join());
            assertThrows(IllegalArgumentException.class,
                    () -> storage.save(root.resolve("absolute.json"), new Document()).join());
        }
    }

    @Test
    void failedEncodingKeepsPreviousDocumentReadable() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            var storage = new JacksonStorageService(root, executor, new NoopLogger());
            var path = Path.of("state.json");
            var original = new Document();
            original.value = "stable";
            storage.save(path, original).join();
            var bytes = Files.readAllBytes(root.resolve(path));

            assertThrows(Exception.class, () -> storage.save(path, new BrokenDocument()).join());
            assertArrayEquals(bytes, Files.readAllBytes(root.resolve(path)));
            assertEquals("stable", storage.createIfMissing(path, Document.class, Document::new).join().value);
        }
    }

    @Test
    void concurrentFirstLoadsCreateOneDefaultDocument() {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var storage = new JacksonStorageService(root, executor, new NoopLogger());
            var defaults = new AtomicInteger();
            var path = Path.of("concurrent.json");

            var first = storage.createIfMissing(path, Document.class, () -> {
                defaults.incrementAndGet();
                var value = new Document();
                value.value = "created";
                return value;
            });
            var second = storage.createIfMissing(path, Document.class, () -> {
                defaults.incrementAndGet();
                var value = new Document();
                value.value = "duplicate";
                return value;
            });

            assertEquals("created", first.join().value);
            assertEquals("created", second.join().value);
            assertEquals(1, defaults.get());
        }
    }

    public static final class Document {

        public String value = "";

    }

    public static final class BrokenDocument {

        public String getValue() {
            throw new IllegalStateException("encode");
        }

    }

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
