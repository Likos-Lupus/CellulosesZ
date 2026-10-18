package top.likoslupus.cellulosesz.application.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/**
 * Every leaf in every scope serializer must be present in the template of its owning file, either as
 * an active key or as a commented optional key. This is the guard against a settings class growing a
 * field that nobody can discover in the generated configuration.
 */
class ConfigTemplateCoverageTest {

    @Test
    fun `every schema leaf is documented in its owning template`() {
        ConfigFiles.all.forEach { file ->
            val paths = templatePaths(read(file.templateResource))
            file.scopes.forEach { scope ->
                val missing = leafPaths(scope.serializer.descriptor, "")
                        .filterNot { path -> paths.covers(scope.scope, path) }
                assertTrue(
                    missing.isEmpty(),
                    "template ${file.fileName} is missing [${scope.scope}] keys: $missing",
                )
            }
        }
    }

    private fun leafPaths(descriptor: SerialDescriptor, prefix: String): List<String> =
        buildList {
            for (index in 0 until descriptor.elementsCount) {
                val name = descriptor.getElementName(index)
                val path = if (prefix.isEmpty()) name else "$prefix.$name"
                val child = descriptor.getElementDescriptor(index)
                // A nested settings object is a table; everything else (primitive, enum, list,
                // map) is one assignable key in the template.
                when (child.kind) {
                    StructureKind.CLASS,
                    StructureKind.OBJECT -> addAll(
                        leafPaths(child, path)
                    )

                    else -> add(path)
                }
            }
        }

    private fun templatePaths(text: String): Set<String> {
        val paths = mutableSetOf<String>()
        var activeCurrent = ""
        var commentedCurrent = ""
        val header = Regex("""^\[\[?([^\]]+)\]\]?\s*$""")
        val commentedHeader = Regex("""^#\s*\[\[?([^\]]+)\]\]?\s*$""")
        val activeKey = Regex("""^([A-Za-z0-9_-]+)\s*=""")
        val commentedKey = Regex("""^#\s*("?[A-Za-z0-9_-]+"?)\s*=""")
        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val parsedHeader = header.find(trimmed)
            if (parsedHeader != null) {
                activeCurrent = parsedHeader.groupValues[1]
                commentedCurrent = activeCurrent
                paths += activeCurrent
                return@forEach
            }
            val parsedCommentedHeader = commentedHeader.find(trimmed)
            if (parsedCommentedHeader != null) {
                commentedCurrent = parsedCommentedHeader.groupValues[1]
                paths += commentedCurrent
                return@forEach
            }
            val parsedKey = activeKey.find(trimmed)
            if (parsedKey != null && activeCurrent.isNotEmpty()) {
                paths += "$activeCurrent.${parsedKey.groupValues[1]}"
                return@forEach
            }
            val parsedCommentedKey = commentedKey.find(trimmed)
            if (parsedCommentedKey != null) {
                val table = commentedCurrent.ifEmpty { activeCurrent }
                if (table.isNotEmpty()) {
                    paths += "$table.${parsedCommentedKey.groupValues[1].trim('"')}"
                }
            }
        }
        return paths
    }

    private fun Set<String>.covers(scope: String, path: String): Boolean {
        val full = "$scope.$path"
        return any { candidate ->
            candidate == full || candidate.startsWith("$full.") || candidate.startsWith("$full\"")
        }
    }

    private fun read(resource: String): String =
        checkNotNull(javaClass.getResourceAsStream(resource)) { "missing resource $resource" }
                .use { it.readBytes().decodeToString() }

}
