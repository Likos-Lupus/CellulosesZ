package top.likoslupus.cellulosesz.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

final class JacksonStorageServiceLifecycleTest {

    @TempDir Path root;

    @Test
    void save_beforeStopAccepting_persistsToDiskAfterDrain() {
        var executor = new ManualExecutor();
        var storage = new JacksonStorageService(root, executor, new NoopLogger());

        var targetFile = Path.of("users", "user1.json");
        var data = new TestUserDoc();
        data.name = "Alice";

        // 1. Submit mutation A
        var saveFutureA = storage.save(targetFile, data);
        assertFalse(saveFutureA.isDone());

        // 2. Begin stop-accepting and drain
        storage.stopAccepting();
        var drainFuture = storage.drain();

        // 3. Drain does not finish while A is outstanding
        assertFalse(drainFuture.isDone());

        // 4. Mutation B submitted after stop-accepting is rejected
        var saveFutureB = storage.save(Path.of("users", "user2.json"), data);
        assertTrue(saveFutureB.isCompletedExceptionally());

        // 5. Complete mutation A on the executor
        executor.runAll();

        // 6. Mutation A and drain complete
        saveFutureA.join();
        drainFuture.join();
        assertTrue(drainFuture.isDone());

        // 7. Verify accepted data is actually persisted on disk
        var writtenPath = root.resolve("users").resolve("user1.json");
        assertTrue(Files.isRegularFile(writtenPath));

        var loaded = storage.loadOrDefault(targetFile, TestUserDoc.class, TestUserDoc::new);
        // Note: loadOrDefault after stopAccepting is rejected, which is expected during shutdown:
        assertTrue(loaded.isCompletedExceptionally());

        // But we can verify with a new storage instance reading the disk:
        var directStorage = new JacksonStorageService(root, Runnable::run, new NoopLogger());
        var persisted = directStorage.loadOrDefault(targetFile, TestUserDoc.class, TestUserDoc::new)
                .join();
        assertEquals("Alice", persisted.name);
    }

    @Test
    void closeAndDrain_withOutstandingOperations_drainsBeforeCompletion() {
        var executor = new ManualExecutor();
        var storage = new JacksonStorageService(root, executor, new NoopLogger());

        var saveFuture = storage.save(Path.of("file.json"), new TestUserDoc());
        assertFalse(saveFuture.isDone());

        var drain = storage.closeAsync();
        assertFalse(drain.isDone());

        // New operations must be rejected immediately
        var lateSave = storage.save(Path.of("late.json"), new TestUserDoc());
        assertTrue(lateSave.isCompletedExceptionally());

        executor.runAll();

        saveFuture.join();
        drain.join();
        assertTrue(Files.isRegularFile(root.resolve("file.json")));
    }

    public static final class TestUserDoc {

        public String name = "default";

    }

    private static final class ManualExecutor implements Executor {

        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                tasks.poll().run();
            }
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
