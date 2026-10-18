package cellulosesz.checks

import org.gradle.api.GradleException
import java.io.File

const val COMMAND_CATALOG_START_MARKER = "<!-- BEGIN COMMAND CATALOG -->"
const val COMMAND_CATALOG_END_MARKER = "<!-- END COMMAND CATALOG -->"

private val COMMAND_MODULES = listOf(
    "movement" to "modules/movement",
    "communication" to "modules/communication",
    "administration" to "modules/administration",
    "utility" to "modules/utility",
    "player" to "modules/player",
    "economy" to "modules/economy",
    "application" to "modules/application",
)

/** Documentation-id category (first path segment) to the module that owns its help resources. */
private val CATEGORY_MODULE = mapOf(
    "movement" to "movement",
    "communication" to "communication",
    "administration" to "administration",
    "utility" to "utility",
    "player" to "player",
    "economy" to "economy",
    "core" to "application",
)

fun commandModuleSources(root: File): List<Pair<String, File>> {
    val out = mutableListOf<Pair<String, File>>()
    COMMAND_MODULES.forEach { (name, base) ->
        kotlinFiles(
            root
                    .resolve(base)
                    .resolve("src/main/kotlin")
        ).forEach { out += name to it }
    }
    return out
}

/** The generated Markdown table for the README command catalog. */
fun commandCatalogTable(root: File): String {
    val blockPattern = Regex("""command\(([\s\S]*?)\)\s*\{""")
    val namePattern = Regex("""name\s*=\s*"([^"]+)"""")
    val permissionPattern = Regex("""permission\s*=\s*CommandPermissions\.(\w+)""")
    val documentationPattern = Regex("""documentation\s*=\s*"([^"]+)"""")
    val aliasesPattern = Regex("""aliases\s*=\s*setOf\(([^)]*)\)""")
    val quotedPattern = Regex("\"([^\"]+)\"")

    val permissionPackage = root.resolve(
        "modules/minecraft-core/src/main/kotlin/top/likoslupus/cellulosesz/core/permission"
    )
    val permissionFiles = kotlinFiles(permissionPackage)
    if (permissionFiles.isEmpty()) {
        throw GradleException(
            "no permission sources found under ${permissionPackage.path}; " +
                    "the README command catalog cannot classify command access"
        )
    }
    val accessByConstant = mutableMapOf<String, String>()
    val nodePattern =
        Regex("""val\s+(\w+)\s*:\s*PermissionSpec\s*=\s*(playerNode|moderatorNode|adminNode)\(""")
    permissionFiles.forEach { file ->
        nodePattern.findAll(file.readText()).forEach { match ->
            val constant = match.groupValues[1]
            val access = when (match.groupValues[2]) {
                "playerNode" -> "Player"
                "moderatorNode" -> "Moderator"
                else -> "Admin"
            }
            val previous = accessByConstant.putIfAbsent(constant, access)
            if (previous != null && previous != access) {
                throw GradleException(
                    "conflicting access for permission constant $constant: " +
                            "$previous in an earlier file vs $access in ${file.name}"
                )
            }
        }
    }
    if (accessByConstant.isEmpty()) {
        throw GradleException(
            "no PermissionSpec access definitions found under ${permissionPackage.path}; " +
                    "the README command catalog would label every command as Player"
        )
    }

    data class Row(
        val moduleOrder: Int,
        val name: String,
        val access: String,
        val documentation: String,
        val aliases: List<String>,
    )

    val moduleOrder = COMMAND_MODULES.map { it.first }
    val rows = mutableListOf<Row>()
    rows += Row(
        moduleOrder.indexOf("application"),
        "cellulosesz",
        "Player",
        "core/cellulosesz",
        emptyList()
    )

    commandModuleSources(root).forEach { (module, file) ->
        val text = file.readText()
        blockPattern.findAll(text).forEach { match ->
            val header = match.groupValues[1]
            val name = namePattern.find(header)?.groupValues?.get(1)
                ?: return@forEach
            val documentation = documentationPattern.find(header)?.groupValues?.get(1)
                ?: return@forEach
            val constant = permissionPattern.find(header)?.groupValues?.get(1).orEmpty()
            val aliases = aliasesPattern.find(header)?.groupValues?.get(1)
                    ?.let { inner ->
                        quotedPattern.findAll(inner)
                                .map { it.groupValues[1] }
                                .toList()
                    }
                ?: emptyList()
            val access = when {
                constant.isEmpty() -> "Player"
                else -> accessByConstant[constant]
                    ?: throw GradleException(
                        "no access mapping for CommandPermissions.$constant " +
                                "(command /$name); checked " +
                                permissionFiles.joinToString { it.name }
                    )
            }
            rows += Row(
                moduleOrder.indexOf(module).coerceAtLeast(0),
                name,
                access,
                documentation,
                aliases,
            )
        }
    }

    val header = listOf("Command", "Access", "Documentation")
    val cells = rows
            .sortedWith(compareBy({ it.moduleOrder }, { it.name }))
            .map { row ->
                val aliases = when {
                    row.aliases.isEmpty() -> ""
                    else -> " (alias ${row.aliases.joinToString(", ") { "`/$it`" }})"
                }
                listOf("`/${row.name}`$aliases", row.access, "`${row.documentation}`")
            }
    val widths = header.indices.map { index ->
        maxOf(
            header[index].length,
            cells.maxOfOrNull { it[index].length } ?: 0
        )
    }
    val renderRow = { values: List<String> ->
        values
                .mapIndexed { index, value -> " ${value.padEnd(widths[index])} " }
                .joinToString(
                    "|",
                    prefix = "|",
                    postfix = "|"
                )
    }
    val table = StringBuilder()
    table.appendLine(renderRow(header))
    table.appendLine(
        header.indices.joinToString(
            "|",
            prefix = "|",
            postfix = "|"
        ) { index ->
            ":" + "-".repeat(widths[index] + 1)
        }
    )
    cells.forEach { table.appendLine(renderRow(it)) }
    return table.toString().trimEnd()
}

fun replaceCommandCatalog(existing: String, table: String): String {
    val start = existing.indexOf(COMMAND_CATALOG_START_MARKER)
    val end = existing.indexOf(COMMAND_CATALOG_END_MARKER)
    if (start < 0 || end < 0 || end < start) {
        throw GradleException("README.md is missing the command catalog markers")
    }
    return "${
        existing.substring(
            0,
            start + COMMAND_CATALOG_START_MARKER.length
        )
    }\n\n$table\n\n${existing.substring(end)}"
}

/**
 * Command-declaration coverage: every `command(name = "...")` is unique, every `documentation` id
 * resolves to en_us + zh_cn Markdown, no orphan help documents, and the README catalog is fresh.
 */
fun commandCatalogViolations(root: File): List<String> {
    val violations = mutableListOf<String>()
    val namePattern = Regex("""command\(\s*name\s*=\s*"([^"]+)"""")
    val documentationPattern = Regex("""documentation\s*=\s*"([^"]+)"""")

    val declaredNames = mutableMapOf<String, String>()
    val docIds = mutableSetOf<String>()
    COMMAND_MODULES.forEach { (module, base) ->
        kotlinFiles(
            root
                    .resolve(base)
                    .resolve("src/main/kotlin")
        ).forEach { file ->
            val text = file.readText()
            namePattern.findAll(text).forEach { match ->
                val previous = declaredNames.putIfAbsent(match.groupValues[1], module)
                if (previous != null) {
                    violations += "${match.groupValues[1]}: duplicate command declaration (also in $previous)"
                }
            }
            documentationPattern.findAll(text).forEach { docIds += it.groupValues[1] }
        }
    }

    docIds.forEach { id ->
        val module = CATEGORY_MODULE[id.substringBefore('/')]
        if (module == null) {
            violations += "command documentation '$id' has no module mapping"
            return@forEach
        }

        listOf("en_us", "zh_cn").forEach { lang ->
            val resource = root
                    .resolve("modules")
                    .resolve(module)
                    .resolve("src/main/resources/cellulosesz/help")
                    .resolve(lang)
                    .resolve("$id.md")
            if (!resource.isFile) {
                violations += "$id: missing $lang command documentation"
            }
        }
    }

    COMMAND_MODULES.forEach { (module, base) ->
        val helpRoot = root
                .resolve(base)
                .resolve("src/main/resources/cellulosesz/help")
        val idsByLang = mutableMapOf<String, Set<String>>()
        listOf("en_us", "zh_cn").forEach { lang ->
            val dir = helpRoot.resolve(lang)
            val ids = if (dir.isDirectory) {
                dir.walkTopDown()
                        .filter { it.isFile && it.extension == "md" }
                        .map { it.relativeTo(dir).invariantSeparatorsPath.removeSuffix(".md") }
                        .toSet()
            } else {
                emptySet()
            }
            idsByLang[lang] = ids
            (ids - docIds).forEach {
                violations += "$it: orphan $lang command documentation in $module"
            }
        }
        val en = idsByLang["en_us"]
            ?: emptySet()
        val zh = idsByLang["zh_cn"]
            ?: emptySet()
        (en - zh).forEach { violations += "$it: command documentation missing zh_cn" }
        (zh - en).forEach { violations += "$it: command documentation missing en_us" }
    }

    val readme = root.resolve("README.md")
    if (readme.isFile) {
        val existing = readme.readText()
        val start = existing.indexOf(COMMAND_CATALOG_START_MARKER)
        val end = existing.indexOf(COMMAND_CATALOG_END_MARKER)
        if (start < 0 || end < 0) {
            violations += "README.md is missing the command catalog markers"
        } else {
            val current = existing.substring(start + COMMAND_CATALOG_START_MARKER.length, end)
            val parseRows = { text: String ->
                text.lineSequence()
                        .map { it.trim() }
                        .filter { it.startsWith("|") }
                        .map { line ->
                            line
                                    .trim('|')
                                    .split("|")
                                    .map { cell -> cell.trim() }
                        }
                        .filter { cells ->
                            cells.any { it.isNotEmpty() }
                                    && cells.none { it.matches(Regex(":?-+:?")) }
                        }
                        .toList()
            }
            if (parseRows(current) != parseRows(commandCatalogTable(root))) {
                violations += "README command catalog is stale; run ./gradlew generateCommandCatalog"
            }
        }
    }
    return violations
}
