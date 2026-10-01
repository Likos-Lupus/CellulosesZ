package top.likoslupus.cellulosesz.config

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ConfigServiceTest {

    @TempDir
    lateinit var directory: Path

    private fun configFile(): Path = directory.resolve("cellulosesz.jsonc")

    @Test
    fun `creates default file on first load`() = runBlocking {
        val service = ConfigService(configFile())
        val result = service.loadOrCreate()

        assertInstanceOf(ConfigReloadResult.Success::class.java, result)
        assertTrue(Files.exists(configFile()))
        assertEquals(CURRENT_SCHEMA_VERSION, service.current.schemaVersion)
    }

    @Test
    fun `reload swaps snapshot and increments generation`() = runBlocking {
        val file = configFile()
        Files.writeString(
            file,
            ConfigCodec.encode(CellulosesConfig(homes = HomeConfig(maxPerPlayer = 4))),
        )
        val service = ConfigService(file)

        val first = service.loadOrCreate() as ConfigReloadResult.Success
        Files.writeString(
            file,
            ConfigCodec.encode(CellulosesConfig(homes = HomeConfig(maxPerPlayer = 7))),
        )
        val second = service.reload() as ConfigReloadResult.Success

        assertEquals(7, service.current.homes.maxPerPlayer)
        assertTrue(second.generation > first.generation)
    }

    @Test
    fun `failed reload keeps previous snapshot and generation`() = runBlocking {
        val file = configFile()
        Files.writeString(
            file,
            ConfigCodec.encode(CellulosesConfig(homes = HomeConfig(maxPerPlayer = 5)))
        )
        val service = ConfigService(file)
        val loaded = service.loadOrCreate() as ConfigReloadResult.Success

        Files.writeString(
            file,
            /* language=JSON */ """{ "homes": { "maxPerPlayer": -3 } }"""
        )
        val failure = service.reload()

        assertInstanceOf(ConfigReloadResult.Failure::class.java, failure)
        assertEquals(5, service.current.homes.maxPerPlayer)
        assertEquals(loaded.generation, service.generation)
    }

}
