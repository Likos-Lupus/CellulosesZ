package top.likoslupus.cellulosesz.communication.page

import top.likoslupus.cellulosesz.communication.config.ServerPageSettings
import top.likoslupus.cellulosesz.core.text.document.MarkdownDocumentException
import top.likoslupus.cellulosesz.core.text.document.MarkdownDocumentParser
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * Reads Markdown server pages from `<configDirectory>/<rootDirectory>/<lang>/<page>.md`. It never
 * caches, so editing a file takes effect on the next view (hot reload) and a config change to the
 * root directory or caps applies immediately. All access is blocking file IO; the service moves it
 * off the server thread.
 */
internal class ServerPageRepository(
    private val configDirectory: Path,
    private val settings: () -> ServerPageSettings,
) {

    fun load(pageName: String, language: LanguageId): ServerPageResult {
        val config = settings()
        val name = pageName.trim().lowercase(Locale.ROOT)
        if (!NAME_PATTERN.matches(name)) {
            return ServerPageResult.InvalidName
        }

        val root = configDirectory.resolve(config.rootDirectory)
        if (!Files.isDirectory(root)) {
            return ServerPageResult.RootMissing
        }
        if (countPages(root) > config.maxPages) {
            return ServerPageResult.TooManyPages
        }

        listOf(language, LanguageId.EN_US).distinct().forEach { candidate ->
            val file = root.resolve(candidate.value).resolve("$name.md")
            if (!Files.isRegularFile(file)) {
                return@forEach
            }
            if (Files.size(file) > config.maxPageBytes) {
                return ServerPageResult.TooLarge
            }
            val text = Files.readString(file, StandardCharsets.UTF_8)
            return try {
                ServerPageResult.Found(MarkdownDocumentParser.parse(text))
            } catch (_: MarkdownDocumentException) {
                ServerPageResult.Invalid
            }
        }
        return ServerPageResult.NoSuchPage
    }

    private fun countPages(root: Path): Int =
        Files.walk(root).use { stream ->
            stream.filter {
                Files.isRegularFile(it)
                        && it.fileName.toString().endsWith(".md")
            }.count().toInt()
        }

    private companion object {

        private val NAME_PATTERN = Regex("[a-z0-9_-]+")

    }

}
