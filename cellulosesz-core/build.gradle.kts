import java.util.*

plugins {
    `java-library`
}

dependencies {
    api(project(":cellulosesz-api"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)
    implementation(libs.classgraph)
    implementation(libs.adventure.minimessage)
}

val messageResourceDirectory = layout.projectDirectory.dir("src/main/resources/messages")
val generatedMessageKeysDirectory = layout.buildDirectory.dir("generated/sources/messageKeys/java/main")

val generateMessageKeys by tasks.registering {
    val english = messageResourceDirectory.file("en_us.yml")
    val chinese = messageResourceDirectory.file("zh_cn.yml")
    val output = generatedMessageKeysDirectory.map {
        it.file("top/likoslupus/cellulosesz/core/i18n/GeneratedMessageKeys.java")
    }

    inputs.files(english, chinese)
    outputs.file(output)

    doLast {
        val linePattern = Regex("^\\\"([^\\\"]+)\\\": \\\"(.*)\\\"$")
        val placeholderPattern = Regex("(?<!\\\\)<([A-Za-z_][A-Za-z0-9_-]*)>")
        val formattingTags = setOf(
            "primary", "secondary",
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white",
            "bold", "b", "italic", "i", "underlined", "underline", "u",
            "strikethrough", "st", "obfuscated", "magic", "reset"
        )
        val numericKeyPattern = Regex("\\.(?:error|reply)\\.\\d+$")
        val positionalPlaceholderPattern = Regex("value\\d+")

        fun readCatalog(file: File): LinkedHashMap<String, String> {
            val result = linkedMapOf<String, String>()
            file.readLines(Charsets.UTF_8).forEachIndexed { index, line ->
                if (line.isBlank()) return@forEachIndexed
                val match = linePattern.matchEntire(line)
                    ?: error("Invalid flat message catalog line ${index + 1} in $file")
                val key = match.groupValues[1]
                check(result.putIfAbsent(key, match.groupValues[2]) == null) {
                    "Duplicate message key $key in $file"
                }
            }
            return result
        }

        val englishCatalog = readCatalog(english.asFile)
        val chineseCatalog = readCatalog(chinese.asFile)
        check(englishCatalog.keys == chineseCatalog.keys) {
            "en_us and zh_cn message keys differ"
        }

        englishCatalog.forEach { (key, template) ->
            check(!numericKeyPattern.containsMatchIn(key)) {
                "Numeric message key is forbidden: $key"
            }
            val englishPlaceholders = placeholderPattern.findAll(template)
                .map { it.groupValues[1].lowercase(Locale.ROOT) }
                .filterNot(formattingTags::contains)
                .toSet()
            val chinesePlaceholders = placeholderPattern.findAll(chineseCatalog.getValue(key))
                .map { it.groupValues[1].lowercase(Locale.ROOT) }
                .filterNot(formattingTags::contains)
                .toSet()
            check(englishPlaceholders == chinesePlaceholders) {
                "Placeholder mismatch for $key: $englishPlaceholders != $chinesePlaceholders"
            }
            check(englishPlaceholders.none(positionalPlaceholderPattern::matches)) {
                "Positional placeholder is forbidden for $key: $englishPlaceholders"
            }
        }

        val constants = englishCatalog.keys.associateWith { key ->
            key.uppercase(Locale.ROOT)
                .replace(Regex("[^A-Z0-9]+"), "_")
                .trim('_')
                .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
        }
        check(constants.values.toSet().size == constants.size) {
            "Generated message key constant names collide"
        }

        val target = output.get().asFile
        target.parentFile.mkdirs()
        target.writeText(buildString {
            appendLine("package top.likoslupus.cellulosesz.core.i18n;")
            appendLine()
            appendLine("/** Generated from messages/en_us.yml. Do not edit manually. */")
            appendLine("public final class GeneratedMessageKeys {")
            appendLine()
            constants.forEach { (key, constant) ->
                appendLine("    public static final String $constant = \"$key\";")
            }
            appendLine()
            appendLine("    private GeneratedMessageKeys() {")
            appendLine("    }")
            appendLine()
            appendLine("}")
        }, Charsets.UTF_8)
    }
}

sourceSets.named("main") {
    java.srcDir(generatedMessageKeysDirectory)
}

tasks.named("compileJava") {
    dependsOn(generateMessageKeys)
}
