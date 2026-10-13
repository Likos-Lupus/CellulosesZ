package cellulosesz.checks

import java.io.File

private val HELP_MODULES = listOf(
    "minecraft-core" to "core",
    "movement" to "movement",
    "communication" to "communication",
    "administration" to "administration",
    "utility" to "utility",
    "player" to "player",
)

private val HELP_LANGUAGES = listOf("en_us", "zh_cn")

private val BANNED_ENGLISH = listOf(
    "player(s)",
    "message(s)",
    "moderator(s)",
    "stack(s)",
    "item(s)"
)
private val BANNED_CHINESE = listOf("服务器不认识", "社交监视", "服务器地标")

private val LIST_ITEM_CODE = Regex("^[-*]\\s+`([^`]+)`\\s*$")

private fun markdownFiles(root: File, module: String, language: String): List<File> {
    val dir = root.resolve("modules/$module/src/main/resources/cellulosesz/help/$language")
    if (!dir.isDirectory) return emptyList()
    return dir.walkTopDown().filter { it.isFile && it.extension == "md" }.toList()
}

/** Structural checks for the hand-written command help plus the suspended marketing/AI phrasings. */
fun helpCopyViolations(root: File): List<String> {
    val violations = mutableListOf<String>()

    HELP_MODULES.forEach { (module, name) ->
        HELP_LANGUAGES.forEach { language ->
            markdownFiles(root, module, language).forEach { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val lines = file.readLines()
                val content = lines.filter { it.isNotBlank() }

                if (content.isEmpty()) {
                    violations += "$relative: help document is empty"
                    return@forEach
                }

                val heading = content.first()
                if (!heading.startsWith("# ")) {
                    violations += "$relative: first line must be a level-1 heading"
                    return@forEach
                }

                val literal = file.nameWithoutExtension
                if (!heading.contains(literal)) {
                    violations += "$relative: heading '$heading' must name the command '$literal'"
                }

                lines.forEachIndexed { index, line ->
                    val code = LIST_ITEM_CODE.find(line.trim())?.groupValues?.get(1)
                    if (code != null && !code.startsWith("/")) {
                        violations += "$relative:${index + 1}: example example '$code' must start with '/'"
                    }
                }

                val banned = if (language == "zh_cn") BANNED_CHINESE else BANNED_ENGLISH
                banned.forEach { phrase ->
                    if (file.readText().contains(phrase)) {
                        violations += "$relative: banned phrasing '$phrase'"
                    }
                }
            }
        }

        HELP_LANGUAGES.forEach { language ->
            val properties = root.resolve(
                "modules/$module/src/main/resources/cellulosesz/i18n/$name/messages_$language.properties"
            )
            if (!properties.isFile) return@forEach
            val text = properties.readText()
            val banned = if (language == "zh_cn") BANNED_CHINESE else BANNED_ENGLISH
            banned.forEach { phrase ->
                if (text.contains(phrase)) {
                    violations += "${properties.relativeTo(root).invariantSeparatorsPath}: banned phrasing '$phrase'"
                }
            }
        }
    }

    return violations
}
