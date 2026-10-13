package top.likoslupus.cellulosesz.communication.page

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.communication.config.ServerPageSettings
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.nio.file.Files
import java.nio.file.Path

class ServerPageRepositoryTest {

    @TempDir
    lateinit var configDirectory: Path

    private fun repository(settings: ServerPageSettings): ServerPageRepository =
        ServerPageRepository(configDirectory) { settings }

    private fun writePage(language: String, page: String, content: String) {
        val directory = configDirectory.resolve("pages").resolve(language)
        Files.createDirectories(directory)
        Files.writeString(directory.resolve("$page.md"), content)
    }

    @Test
    fun `loads a page in the requested language`() {
        writePage("en_us", "motd", "# Welcome")
        writePage("zh_cn", "motd", "# 欢迎")

        val result = repository(ServerPageSettings())
                .load("motd", LanguageId.ZH_CN)

        val found = assertInstanceOf(
            ServerPageResult.Found::class.java,
            result
        )
        assertEquals(1, found.document.blocks.size)
    }

    @Test
    fun `falls back to en_us when the requested language is missing`() {
        writePage("en_us", "rules", "# Rules")

        val result = repository(ServerPageSettings())
                .load("rules", LanguageId.ZH_CN)

        assertInstanceOf(ServerPageResult.Found::class.java, result)
    }

    @Test
    fun `reports a missing page`() {
        Files.createDirectories(configDirectory.resolve("pages/en_us"))

        assertEquals(
            ServerPageResult.NoSuchPage,
            repository(ServerPageSettings())
                    .load("motd", LanguageId.EN_US),
        )
    }

    @Test
    fun `reports an unsafe page name`() {
        Files.createDirectories(configDirectory.resolve("pages/en_us"))

        assertEquals(
            ServerPageResult.InvalidName,
            repository(ServerPageSettings())
                    .load("../secret", LanguageId.EN_US),
        )
    }

    @Test
    fun `reports a missing pages directory`() {
        assertEquals(
            ServerPageResult.RootMissing,
            repository(ServerPageSettings())
                    .load("motd", LanguageId.EN_US),
        )
    }

    @Test
    fun `reports a page that exceeds the byte cap`() {
        writePage("en_us", "info", "x".repeat(64))

        assertEquals(
            ServerPageResult.TooLarge,
            repository(ServerPageSettings(maxPageBytes = 16))
                    .load("info", LanguageId.EN_US),
        )
    }

    @Test
    fun `reports too many pages`() {
        writePage("en_us", "one", "# One")
        writePage("en_us", "two", "# Two")

        assertEquals(
            ServerPageResult.TooManyPages,
            repository(ServerPageSettings(maxPages = 1))
                    .load("one", LanguageId.EN_US),
        )
    }

    @Test
    fun `reports invalid markdown`() {
        writePage("en_us", "bad", "<div>raw html</div>")

        assertEquals(
            ServerPageResult.Invalid,
            repository(ServerPageSettings())
                    .load("bad", LanguageId.EN_US),
        )
    }

}
