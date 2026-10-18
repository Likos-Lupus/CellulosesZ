package top.likoslupus.cellulosesz.application.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.communication.config.MessagingSettings
import top.likoslupus.cellulosesz.foundation.config.BundleConfigStore
import top.likoslupus.cellulosesz.foundation.config.ConfigReloadResult
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers

class DefaultTemplateTest {

    @TempDir
    lateinit var directory: Path

    private fun store(): BundleConfigStore<CellulosesConfig> =
        ConfigFiles.store(directory, Dispatchers.IO) { _, _ -> emptyList() }

    @Test
    fun `packaged templates decode to the Kotlin defaults`() {
        ConfigDefaults.writeIfMissing(directory, ConfigFiles.all)

        val store = store()
        val result = store.initialLoadBlocking()

        assertInstanceOf(ConfigReloadResult.Success::class.java, result)
        assertEquals(CellulosesConfig(), store.current)
        val written = Files.list(directory).use { stream ->
            stream.map { it.fileName.toString() }.toList()
        }
        assertEquals(ConfigFiles.all.map { it.fileName }.toSet(), written.toSet())
    }

    @Test
    fun `a table owned by another file is ignored`() {
        ConfigDefaults.writeIfMissing(directory, ConfigFiles.all)
        Files.writeString(
            directory.resolve("movement.toml"),
            /* language=TOML */
            """
            [messaging]
            enabled = false

            [movement]
            [movement.teleport]
            delaySeconds = 3
            """.trimIndent(),
        )

        val store = store()
        assertInstanceOf(
            ConfigReloadResult.Success::class.java,
            store.initialLoadBlocking()
        )

        assertEquals(3, store.current.movement.teleport.delaySeconds)
        assertEquals(MessagingSettings().enabled, store.current.messaging.enabled)
    }

    @Test
    fun `an unknown top-level table fails with its file name`() {
        ConfigDefaults.writeIfMissing(directory, ConfigFiles.all)
        Files.writeString(
            directory.resolve("movement.toml"),
            /* language=TOML */
            """
            [movment]
            enabled = true
            """.trimIndent(),
        )

        val failure = assertInstanceOf(
            ConfigReloadResult.Failure::class.java,
            store().initialLoadBlocking(),
        )

        assertTrue(
            failure.errors.any {
                it.source == "movement.toml" && it.message.contains("unknown top-level")
            },
            "expected a file-scoped unknown-table error, got ${failure.errors}",
        )
    }

    @Test
    fun `an unknown key inside the owning scope still fails`() {
        ConfigDefaults.writeIfMissing(directory, ConfigFiles.all)
        Files.writeString(
            directory.resolve("movement.toml"),
            /* language=TOML */
            """
            [movement.teleport]
            delaySecond = 3
            """.trimIndent(),
        )

        val failure = assertInstanceOf(
            ConfigReloadResult.Failure::class.java,
            store().initialLoadBlocking(),
        )

        assertTrue(
            failure.errors.any { it.source == "movement.toml" },
            "expected a file-scoped decode error, got ${failure.errors}",
        )
    }

    @Test
    fun `a missing module file falls back to that scope's defaults`() {
        ConfigDefaults.writeIfMissing(directory, ConfigFiles.all)
        Files.delete(directory.resolve("economy.toml"))

        val store = store()
        assertInstanceOf(
            ConfigReloadResult.Success::class.java,
            store.initialLoadBlocking()
        )

        assertEquals(CellulosesConfig().economy, store.current.economy)
        assertEquals(CellulosesConfig().movement, store.current.movement)
    }

}
