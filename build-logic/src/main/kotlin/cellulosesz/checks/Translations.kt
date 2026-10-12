package cellulosesz.checks

import java.io.File

private val TRANSLATION_MODULES = listOf(
    "minecraft-core" to "core",
    "movement" to "movement",
    "communication" to "communication",
    "administration" to "administration",
    "utility" to "utility",
)

private val PLACEHOLDER = Regex("\\{(\\d+)}")

private fun placeholders(value: String): Set<Int> =
    PLACEHOLDER.findAll(value)
            .map { it.groupValues[1].toInt() }
            .toSet()

private fun loadMessages(file: File): Map<String, String> {
    val result = LinkedHashMap<String, String>()
    file.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
        val index = trimmed.indexOf('=')
        if (index <= 0) return@forEach
        result[trimmed.substring(0, index).trim()] = trimmed.substring(index + 1).trim()
    }
    return result
}

/** Identical key + placeholder sets per language, and no duplicate key across modules. */
fun translationViolations(root: File): List<String> {
    val violations = mutableListOf<String>()
    val seenKeys = mutableMapOf<String, String>()
    TRANSLATION_MODULES.forEach { (module, name) ->
        val en = root.resolve("modules/$module/src/main/resources/cellulosesz/i18n/$name/messages_en_us.properties")
        val zh = root.resolve("modules/$module/src/main/resources/cellulosesz/i18n/$name/messages_zh_cn.properties")
        if (!en.isFile || !zh.isFile) {
            violations += "modules/$module: missing en_us/zh_cn translation resources"
            return@forEach
        }

        val enMap = loadMessages(en)
        val zhMap = loadMessages(zh)
        (enMap.keys - zhMap.keys).forEach { violations += "modules/$module: key '$it' missing from zh_cn" }
        (zhMap.keys - enMap.keys).forEach { violations += "modules/$module: key '$it' missing from en_us" }
        enMap.keys.intersect(zhMap.keys).forEach { key ->
            if (placeholders(enMap.getValue(key)) != placeholders(zhMap.getValue(key))) {
                violations += "modules/$module: placeholder mismatch for '$key'"
            }
            val owner = seenKeys[key]
            if (owner != null) {
                violations += "duplicate translation key '$key' in modules/$module and $owner"
            } else {
                seenKeys[key] = "modules/$module"
            }
        }
    }
    return violations
}
