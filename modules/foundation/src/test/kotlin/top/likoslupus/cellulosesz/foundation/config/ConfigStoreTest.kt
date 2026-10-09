package top.likoslupus.cellulosesz.foundation.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ConfigStoreTest {

    @Serializable
    private data class Sample(
        val value: Int = 1
    )

    @TempDir
    lateinit var directory: Path

    private fun store(
        path: Path,
        transition: (Sample, Sample) -> List<ValidationError> = { _, _ -> emptyList() },
    ): ConfigStore<Sample> =
        ConfigStore(
            path = path,
            codec = TomlConfigCodec(Sample.serializer()),
            validate = {
                if (it.value < 0) {
                    listOf(ValidationError("value", "must be >= 0"))
                } else {
                    emptyList()
                }
            },
            ioDispatcher = Dispatchers.IO,
            validateTransition = transition,
        )

    @Test
    fun `current throws before initial load`() {
        val store = store(directory.resolve("config.toml"))
        assertFalse(store.initialized)
        assertThrows(IllegalStateException::class.java) { store.current }
    }

    @Test
    fun `initial load reads an existing file`() {
        val file = directory.resolve("config.toml")
        Files.writeString(file, "value = 4\n")
        val store = store(file)

        val result = store.initialLoadBlocking()

        assertInstanceOf(
            ConfigReloadResult.Success::class.java,
            result
        )
        assertEquals(4, store.current.value)
        assertEquals(1, store.generation)
    }

    @Test
    fun `missing file fails initial load`() {
        val result = store(
            directory.resolve("config.toml")
        ).initialLoadBlocking()
        assertInstanceOf(
            ConfigReloadResult.Failure::class.java,
            result
        )
    }

    @Test
    fun `reload swaps snapshot and increments generation`() =
        runBlocking {
            val file = directory.resolve("config.toml")
            Files.writeString(file, "value = 4\n")
            val store = store(file)
            val first = store.initialLoadBlocking() as ConfigReloadResult.Success

            Files.writeString(file, "value = 7\n")
            val second = store.reload() as ConfigReloadResult.Success

            assertEquals(7, store.current.value)
            assertTrue(second.generation > first.generation)
        }

    @Test
    fun `failed reload keeps previous snapshot and generation`() =
        runBlocking {
            val file = directory.resolve("config.toml")
            Files.writeString(file, "value = 5\n")
            val store = store(file)
            val loaded = store.initialLoadBlocking() as ConfigReloadResult.Success

            Files.writeString(file, "value = -3\n")
            val failure = store.reload()

            assertInstanceOf(
                ConfigReloadResult.Failure::class.java,
                failure
            )
            assertEquals(5, store.current.value)
            assertEquals(loaded.generation, store.generation)
        }

    @Test
    fun `rejected transition keeps previous snapshot`() =
        runBlocking {
            val file = directory.resolve("config.toml")
            Files.writeString(file, "value = 1\n")
            val store = store(file) { _, candidate ->
                if (candidate.value == 2) listOf(
                    ValidationError(
                        "value",
                        "restart required"
                    )
                ) else emptyList()
            }
            store.initialLoadBlocking()

            Files.writeString(file, "value = 2\n")
            val failure = store.reload()

            assertInstanceOf(
                ConfigReloadResult.Failure::class.java,
                failure
            )
            assertEquals(1, store.current.value)
        }

    @Test
    fun `unknown keys are rejected`() {
        val file = directory.resolve("config.toml")
        Files.writeString(file, "value = 1\nunknown = 2\n")
        val result = store(file).initialLoadBlocking()
        assertInstanceOf(
            ConfigReloadResult.Failure::class.java,
            result
        )
    }

}
