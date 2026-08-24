package top.likoslupus.cellulosesz.core.module

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.core.command.CommandMiddleware
import top.likoslupus.cellulosesz.core.command.execution.DefaultCommandExecutionPipeline
import top.likoslupus.cellulosesz.core.config.JacksonConfigRegistry
import top.likoslupus.cellulosesz.core.event.SimpleEventRegistry
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger
import top.likoslupus.cellulosesz.core.scheduler.DefaultScheduler
import top.likoslupus.cellulosesz.core.service.DefaultServiceRegistry
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class OwnerBoundRegistryTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `close when registrations owned revokes all side effects`() = runTest {
        val logger = NoopLogger()
        val scope = DefaultModuleScope("module")
        val events = ModuleScopedEventRegistry("module", SimpleEventRegistry(), scope)
        val pipeline = DefaultCommandExecutionPipeline(logger, DefaultServiceRegistry())
        val middlewares = ModuleScopedCommandMiddlewareRegistry("module", pipeline, scope)
        val schedulerDelegate = DefaultScheduler(logger)
        val scheduler = ModuleScopedScheduler("module", schedulerDelegate, scope)
        val configDelegate = JacksonConfigRegistry(root, logger)
        val configs = ModuleScopedConfigRegistry("module", configDelegate, scope)
        val calls = AtomicInteger()

        events.listen(String::class.java) { calls.incrementAndGet() }
        val middleware = CommandMiddleware { _, _, next -> next.proceed() }
        middlewares.addMiddleware(middleware)
        scheduler.syncRepeating({ calls.incrementAndGet() }, 0L, 1L)
        configs.register("module.test", Document::class.java, "module.yml", ::Document)

        events.fire("before")
        schedulerDelegate.tick()
        assertEquals(2, calls.get())
        assertEquals(1, pipeline.middlewares().size)
        assertNotNull(configs.require("module.test", Document::class.java))

        scope.close()
        events.fire("after")
        schedulerDelegate.tick()
        assertEquals(2, calls.get())
        assertEquals(0, pipeline.middlewares().size)
        assertTrue(configDelegate.optional("module.test", Document::class.java).isEmpty)

        val replacement = configDelegate.register(
            "module.test",
            Document::class.java,
            "module.yml",
            ::Document,
            "module"
        )
        replacement.close()
        schedulerDelegate.close()
    }

    class Document {

        var value: String = "value"

    }

    private class NoopLogger : CellulosesZLogger {

        override fun warn(message: String) = Unit
        override fun error(message: String) = Unit
        override fun error(message: String, throwable: Throwable) = Unit
        override fun info(message: String) = Unit

    }

}
