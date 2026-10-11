package top.likoslupus.cellulosesz.core.text.document

import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads and caches bundled command documents. Documents are explicit resources (never a classpath
 * scan) at `cellulosesz/help/<language>/<id>.md`, read through the [sources]; each source reads
 * from the class loader of the module that owns those documents, so a document is still found when
 * modules are separate jars at runtime. A missing document is a `null` lookup; the help service
 * maps it to a localized fallback line.
 */
public class MarkdownDocumentLoader(
    private val sources: List<CommandDocumentSource>,
) {

    private val cache: ConcurrentHashMap<Key, Result<MarkdownDocument?>> = ConcurrentHashMap()

    public fun load(id: CommandDocumentId, language: LanguageId): MarkdownDocument? {
        val key = Key(id.value, language.value)
        return cache.computeIfAbsent(key) { loadUncached(id, language) }.getOrNull()
    }

    private fun loadUncached(
        id: CommandDocumentId,
        language: LanguageId
    ): Result<MarkdownDocument?> =
        runCatching {
            val text = sources.firstNotNullOfOrNull { it.read(id, language) }
                ?: return@runCatching null
            MarkdownDocumentParser.parse(text)
        }

    private data class Key(
        val id: String,
        val language: String,
    )

}
