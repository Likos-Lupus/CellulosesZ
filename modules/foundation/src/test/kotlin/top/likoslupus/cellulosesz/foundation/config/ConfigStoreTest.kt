package top.likoslupus.cellulosesz.foundation.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.nio.file.Files
import java.nio.file.Path

class ConfigStoreTest {

    @Serializable
    private data class Sample(val value: Int = 1)

    @TempDir
    lateinit var directory: Path

    private fun store(path: Path): ConfigStore<Sample> =
        ConfigStore(
            path = path,
            serializer = Sample.serializer(),
            defaultValue = { Sample() },
            validate = {
                if (it.value < 0) {
                    listOf(ValidationError("value", "must be >= 0"))
                } else {
                    emptyList()
                }
            },
            ioDispatcher = Dispatchers.IO,
        )

    private fun encode(sample: Sample): String =
        StorageJson.format.encodeToString(
            Sample.serializer(),
            sample
        )

    @Test
    fun `creates default file on first load`() =
        runBlocking {
            val file = directory.resolve("config.jsonc")
            val store = store(file)

            val result = store.loadOrCreate()

            assertInstanceOf(
                ConfigReloadResult.Success::class.java,
                result
            )
            assertTrue(Files.exists(file))
            assertEquals(
                1,
                store.current.value
            )
        }

    @Test
    fun `reload swaps snapshot and increments generation`() =
        runBlocking {
            val file = directory.resolve("config.jsonc")
            Files.writeString(file, encode(Sample(4)))
            val store = store(file)

            val first = store.loadOrCreate() as ConfigReloadResult.Success
            Files.writeString(file, encode(Sample(7)))
            val second = store.reload() as ConfigReloadResult.Success

            assertEquals(7, store.current.value)
            assertTrue(second.generation > first.generation)
        }

    @Test
    fun `failed reload keeps previous snapshot and generation`() =
        runBlocking {
            val file = directory.resolve("config.jsonc")
            Files.writeString(file, encode(Sample(5)))
            val store = store(file)
            val loaded = store.loadOrCreate() as ConfigReloadResult.Success

            Files.writeString(file, """{ "value": -3 }""")
            val failure = store.reload()

            assertInstanceOf(
                ConfigReloadResult.Failure::class.java,
                failure
            )
            assertEquals(
                5,
                store.current.value
            )
            assertEquals(
                loaded.generation,
                store.generation
            )
        }

}
