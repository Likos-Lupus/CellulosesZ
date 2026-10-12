package top.likoslupus.cellulosesz.core.text.document

import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads and caches bundled command documents. Documents are explicit resources (never a classpath
 * scan) at `cellulosesz/help/<language>/<id>.md`, read through the [sources]; each source reads from
 * the class loader of the module that owns those documents, so a document is still found when modules
 * are separate jars at runtime.
 */
public class MarkdownDocumentLoader(
    private val sources: List<CommandDocumentSource>,
) {

    private val cache: ConcurrentHashMap<Key, DocumentLoadResult> = ConcurrentHashMap()

    public fun load(id: CommandDocumentId, language: LanguageId): DocumentLoadResult {
        val key = Key(id.value, language.value)
        return cache.computeIfAbsent(key) { loadUncached(id, language) }
    }

    private fun loadUncached(
        id: CommandDocumentId,
        language: LanguageId,
    ): DocumentLoadResult {
        val text = sources.firstNotNullOfOrNull { it.read(id, language) }
            ?: return DocumentLoadResult.Missing
        return try {
            DocumentLoadResult.Found(MarkdownDocumentParser.parse(text))
        } catch (exception: MarkdownDocumentException) {
            DocumentLoadResult.Invalid(exception.message ?: "invalid document")
        }
    }

    private data class Key(
        val id: String,
        val language: String,
    )

}
