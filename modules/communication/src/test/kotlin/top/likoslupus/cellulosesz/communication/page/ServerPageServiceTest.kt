package top.likoslupus.cellulosesz.communication.page

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.communication.config.ServerPageSettings
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.nio.file.Path

class ServerPageServiceTest {

    @TempDir
    lateinit var configDirectory: Path

    @Test
    fun `returns disabled without touching the filesystem`() =
        runBlocking {
            val disabled = ServerPageSettings(enabled = false)
            val service = ServerPageService(
                repository = ServerPageRepository(configDirectory) { disabled },
                settings = { disabled },
            )

            assertEquals(
                ServerPageResult.Disabled,
                service.load("motd", LanguageId.EN_US),
            )
        }

}
