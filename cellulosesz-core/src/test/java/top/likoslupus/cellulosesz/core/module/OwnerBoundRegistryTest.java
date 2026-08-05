package top.likoslupus.cellulosesz.core.module;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.core.command.execution.DefaultCommandExecutionPipeline;
import top.likoslupus.cellulosesz.core.config.JacksonConfigRegistry;
import top.likoslupus.cellulosesz.core.event.SimpleEventRegistry;
import top.likoslupus.cellulosesz.core.scheduler.DefaultScheduler;
import top.likoslupus.cellulosesz.core.service.DefaultServiceRegistry;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class OwnerBoundRegistryTest {

    @TempDir Path root;

    @Test
    void close_whenRegistrationsOwned_revokesAllSideEffects() {
        var logger = new NoopLogger();
        var scope = new DefaultModuleScope("module");
        var events = new ModuleScopedEventRegistry("module", new SimpleEventRegistry(), scope);
        var pipeline = new DefaultCommandExecutionPipeline(logger, new DefaultServiceRegistry());
        var middlewares = new ModuleScopedCommandMiddlewareRegistry("module", pipeline, scope);
        var schedulerDelegate = new DefaultScheduler(logger);
        var scheduler = new ModuleScopedScheduler("module", schedulerDelegate, scope);
        var configDelegate = new JacksonConfigRegistry(root, logger);
        var configs = new ModuleScopedConfigRegistry("module", configDelegate, scope);
        var calls = new AtomicInteger();

        events.listen(String.class, _ -> calls.incrementAndGet());
        var middleware = (CommandMiddleware) (
                _,
                _,
                next
        ) -> next.proceed();
        middlewares.addMiddleware(middleware);
        scheduler.syncRepeating(calls::incrementAndGet, 0L, 1L);
        configs.register("module.test", Document.class, "module.yml", Document::new);

        events.fire("before");
        schedulerDelegate.tick();
        assertEquals(2, calls.get());
        assertEquals(1, pipeline.middlewares().size());
        assertNotNull(configs.require("module.test", Document.class));

        scope.closeAsync().join();
        events.fire("after");
        schedulerDelegate.tick();
        assertEquals(2, calls.get());
        assertEquals(0, pipeline.middlewares().size());
        assertTrue(configDelegate.optional("module.test", Document.class).isEmpty());

        var replacement = configDelegate.register(
                "module.test",
                Document.class,
                "module.yml",
                Document::new,
                "module"
        );
        replacement.close();
        schedulerDelegate.close();
    }

    public static final class Document {

        public String value = "value";

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
