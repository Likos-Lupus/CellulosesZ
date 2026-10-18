package top.likoslupus.cellulosesz.application.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * In-file documentation contract: every file declares its scope/owner/reload policy, and every
 * active table and key is preceded by a comment. Optional keys are additionally checked by
 * [ConfigTemplateCoverageTest], which accepts commented keys.
 */
class ConfigTemplateDocumentationTest {

    @Test
    fun `every file declares its scope and documents every active key`() {
        ConfigFiles.all.forEach { file ->
            val text = read(file.templateResource)
            assertTrue(
                text.contains("Scope:"),
                "${file.fileName}: missing Scope header"
            )
            assertTrue(
                text.contains("Owner:"),
                "${file.fileName}: missing Owner header"
            )
            assertTrue(
                text.contains("Reload:"),
                "${file.fileName}: missing Reload header"
            )

            val lines = text.lines()
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEachIndexed
                }
                if (trimmed.startsWith("[")) {
                    assertTrue(
                        previousNonBlankIsComment(lines, index),
                        "${file.fileName}:${index + 1}: table has no comment above: $trimmed",
                    )
                    return@forEachIndexed
                }
                if (trimmed.contains("=")) {
                    assertTrue(
                        previousNonBlankIsComment(lines, index),
                        "${file.fileName}:${index + 1}: key has no comment above: $trimmed",
                    )
                }
            }
        }
    }

    private fun previousNonBlankIsComment(lines: List<String>, index: Int): Boolean {
        var cursor = index - 1
        while (cursor >= 0 && lines[cursor].isBlank()) {
            cursor--
        }
        return cursor >= 0 && lines[cursor].trimStart().startsWith("#")
    }

    private fun read(resource: String): String =
        checkNotNull(javaClass.getResourceAsStream(resource)) { "missing resource $resource" }
                .use { it.readBytes().decodeToString() }

}
