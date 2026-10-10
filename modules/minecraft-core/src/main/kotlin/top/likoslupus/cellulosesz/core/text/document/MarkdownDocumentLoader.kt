package top.likoslupus.cellulosesz.core.text.document

import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads and caches bundled command documents from the classpath. Documents are explicit resources
 * (never a classpath scan) at `cellulosesz/help/<language>/<id>.md`. A missing document is a
 * `null` lookup; the help service maps it to a localized fallback line.
 */
public class MarkdownDocumentLoader {

    private val cache: ConcurrentHashMap<Key, Result<MarkdownDocument?>> = ConcurrentHashMap()

    public fun load(id: CommandDocumentId, language: LanguageId): MarkdownDocument? {
        val key = Key(id.value, language.value)
        return cache.computeIfAbsent(key) { loadUncached(it) }.getOrNull()
    }

    private fun loadUncached(key: Key): Result<MarkdownDocument?> =
        runCatching {
            val resource = "/cellulosesz/help/${key.language}/${key.id}.md"
            val stream = javaClass.getResourceAsStream(resource)
                ?: return@runCatching null
            val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            MarkdownDocumentParser.parse(text)
        }

    private data class Key(
        val id: String,
        val language: String,
    )

}
