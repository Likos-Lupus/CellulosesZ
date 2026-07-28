package top.likoslupus.cellulosesz.core.scheduler;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DefaultSchedulerTest {

    @Test
    void oneFailingTaskDoesNotStopOtherTasksOrRepeatingTasks() {
        var logger = new RecordingLogger();
        var scheduler = new DefaultScheduler(logger);
        var calls = new AtomicInteger();
        scheduler.sync(() -> { throw new IllegalStateException("expected"); });
        scheduler.sync(calls::incrementAndGet);
        scheduler.syncRepeating(calls::incrementAndGet, 0, 1);
        scheduler.tick();
        scheduler.tick();
        assertEquals(3, calls.get());
        assertEquals(1, logger.errors.get());
        scheduler.close();
    }

    private static final class RecordingLogger implements CellulosesZLogger {
        private final AtomicInteger errors = new AtomicInteger();
        @Override public void warn(String message) { }
        @Override public void error(String message) { errors.incrementAndGet(); }
        @Override public void error(String message, Throwable throwable) { errors.incrementAndGet(); }
        @Override public void info(String message) { }
    }
}
