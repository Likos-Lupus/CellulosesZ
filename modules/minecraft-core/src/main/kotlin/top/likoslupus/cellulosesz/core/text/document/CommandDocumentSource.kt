package top.likoslupus.cellulosesz.core.text.document

import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.nio.charset.StandardCharsets

/**
 * Reads a bundled command document's raw Markdown for a language. Each source reads through a
 * specific class loader so documents stay reachable even when the owning module is loaded as its
 * own jar/class loader (as the development runtime does).
 */
public fun interface CommandDocumentSource {

    public fun read(id: CommandDocumentId, language: LanguageId): String?

}

/**
 * Reads `cellulosesz/help/<language>/<id>.md` from the class loader that defined [anchor]. Anchoring
 * on a class of the owning module keeps the lookup inside that module's class loader instead of the
 * reader's, which is required when modules are not flattened into one jar.
 */
public class ClasspathCommandDocumentSource(private val anchor: Class<*>) : CommandDocumentSource {

    override fun read(id: CommandDocumentId, language: LanguageId): String? =
        anchor.getResourceAsStream("/cellulosesz/help/${language.value}/${id.value}.md")
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }

}
