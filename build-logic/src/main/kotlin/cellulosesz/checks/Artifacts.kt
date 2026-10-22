package cellulosesz.checks

import org.gradle.api.GradleException
import java.io.File
import java.util.zip.ZipFile

private fun distributionJar(directory: File, marker: String): File =
    directory.listFiles()?.firstOrNull {
        it.name.endsWith("$marker.jar")
                && !it.name.contains("sources")
    } ?: throw GradleException("missing $marker distribution jar in $directory")

private fun entriesOf(jar: File): Set<String> =
    ZipFile(jar).use { zip ->
        zip.entries()
                .asSequence()
                .map { it.name }
                .toSet()
    }

private fun entryText(jar: File, entry: String): String? =
    ZipFile(jar).use { zip ->
        zip.getEntry(entry)?.let {
            zip.getInputStream(it).readBytes().decodeToString()
        }
    }

/** Inspects both loader distributions (flattened modules, nested runtime jars, resources). */
fun artifactViolations(root: File): List<String> {
    val violations = mutableListOf<String>()
    val fabricJar = distributionJar(root.resolve("versions/26.1.2-fabric/build/libs"), "fabric")
    val neoJar = distributionJar(root.resolve("versions/26.1.2-neoforge/build/libs"), "neoforge")
    val fabricEntries = entriesOf(fabricJar)
    val neoEntries = entriesOf(neoJar)

    val requiredFlattened = listOf(
        "top/likoslupus/cellulosesz/application/bootstrap/CellulosesZ.class",
        "top/likoslupus/cellulosesz/movement/home/HomeService.class",
        "top/likoslupus/cellulosesz/utility/kit/JdbcKitRepository.class",
        "top/likoslupus/cellulosesz/player/PlayerFeature.class",
        "top/likoslupus/cellulosesz/foundation/database/HikariDatabaseRuntime.class",
        "top/likoslupus/cellulosesz/administration/mixin/ServerPlayerVisibilityMixin.class",
        "top/likoslupus/cellulosesz/administration/mixin/CommandsRestrictionMixin.class",
        "top/likoslupus/cellulosesz/player/mixin/PlayerDisplayNameMixin.class",
        "top/likoslupus/cellulosesz/communication/mixin/EmoteCommandsMixin.class",
    )
    requiredFlattened.forEach { entry ->
        if (entry !in fabricEntries) violations += "fabric jar is missing flattened class $entry"
        if (entry !in neoEntries) violations += "neoforge jar is missing flattened class $entry"
    }

    val requiredMixinConfigs = listOf(
        "cellulosesz.administration.mixins.json",
        "cellulosesz.player.mixins.json",
        "cellulosesz.communication.mixins.json",
    )
    requiredMixinConfigs.forEach { entry ->
        if (entry !in fabricEntries) violations += "fabric jar is missing mixin config $entry"
        if (entry !in neoEntries) violations += "neoforge jar is missing mixin config $entry"
    }

    if ("fabric.mod.json" !in fabricEntries) {
        violations += "fabric jar is missing fabric.mod.json"
    }
    val fabricModJson = entryText(fabricJar, "fabric.mod.json").orEmpty()
    if (!Regex("\"environment\"\\s*:\\s*\"\\*\"").containsMatchIn(fabricModJson)) {
        violations += "fabric.mod.json must declare \"environment\": \"*\" (client-loadable, server-gated)"
    }
    if ("META-INF/neoforge.mods.toml" !in neoEntries) {
        violations += "neoforge jar is missing neoforge.mods.toml"
    }
    if (neoEntries.none { it.startsWith("META-INF/jars/kotlin-stdlib") }) {
        violations += "neoforge jar is missing the Kotlin stdlib Jar-in-Jar"
    }

    val requiredNested = listOf("HikariCP", "tomlkt", "commonmark")
    requiredNested.forEach { jar ->
        if (
            fabricEntries.none {
                it.startsWith("META-INF/jars/")
                        && it.contains(jar)
            }
        ) {
            violations += "fabric jar is missing nested runtime jar: $jar"
        }
        if (
            neoEntries.none {
                it.startsWith("META-INF/jars/")
                        && it.contains(jar)
            }
        ) {
            violations += "neoforge jar is missing nested runtime jar: $jar"
        }
    }

    if (
        fabricEntries.none {
            it.startsWith("META-INF/jars/")
                    && it.contains("adventure-platform-fabric")
        }
    ) {
        violations += "fabric jar is missing nested adventure-platform-fabric"
    }
    if (
        neoEntries.none {
            it.startsWith("META-INF/jars/")
                    && it.contains("adventure-platform-neoforge")
        }
    ) {
        violations += "neoforge jar is missing nested adventure-platform-neoforge"
    }
    // JDBC drivers are provisioned at runtime (ADR 0024) and must never be bundled again: a nested
    // driver would silently win over the downloaded one and make the offline bundle pointless.
    val unbundledDrivers = listOf(
        "sqlite-jdbc-",
        "h2-",
        "postgresql-",
        "mariadb-java-client-",
        "mysql-connector-j-",
    )
    (fabricEntries + neoEntries)
            .filter { entry ->
                (entry.startsWith("META-INF/jars/") || entry.startsWith("META-INF/jarjar/")) &&
                        unbundledDrivers.any {
                            entry.substringAfterLast('/').startsWith(it)
                        }
            }
            .forEach { violations += "distribution jar bundles a JDBC driver that must be provisioned: $it" }

    (fabricEntries + neoEntries)
            .filter { it.startsWith("net/kyori/") || it.startsWith("org/commonmark/") }
            .forEach { violations += "distribution jar flattens external class $it" }

    listOf(
        "cellulosesz/help/en_us/core/cellulosesz.md",
        "cellulosesz/help/zh_cn/core/cellulosesz.md",
        "cellulosesz/help/en_us/movement/home.md",
        "cellulosesz/help/zh_cn/movement/home.md",
        "cellulosesz/help/en_us/player/seen.md",
        "cellulosesz/help/zh_cn/player/seen.md",
        "cellulosesz/help/en_us/player/playtime.md",
        "cellulosesz/help/zh_cn/player/playtime.md",
        "cellulosesz/help/en_us/player/whois.md",
        "cellulosesz/help/zh_cn/player/whois.md",
        "cellulosesz/help/en_us/player/afk.md",
        "cellulosesz/help/zh_cn/player/afk.md",
        "cellulosesz/help/en_us/player/ping.md",
        "cellulosesz/help/zh_cn/player/ping.md",
        "cellulosesz/help/en_us/player/near.md",
        "cellulosesz/help/zh_cn/player/near.md",
        "cellulosesz/i18n/core/messages_en_us.properties",
        "cellulosesz/i18n/core/messages_zh_cn.properties",
        "cellulosesz/i18n/movement/messages_en_us.properties",
        "cellulosesz/i18n/movement/messages_zh_cn.properties",
        "cellulosesz/i18n/communication/messages_en_us.properties",
        "cellulosesz/i18n/communication/messages_zh_cn.properties",
        "cellulosesz/i18n/administration/messages_en_us.properties",
        "cellulosesz/i18n/administration/messages_zh_cn.properties",
        "cellulosesz/i18n/utility/messages_en_us.properties",
        "cellulosesz/i18n/utility/messages_zh_cn.properties",
        "cellulosesz/i18n/player/messages_en_us.properties",
        "cellulosesz/i18n/player/messages_zh_cn.properties",
    ).forEach { entry ->
        if (entry !in fabricEntries) {
            violations += "fabric jar is missing resource $entry"
        }
        if (entry !in neoEntries) {
            violations += "neoforge jar is missing resource $entry"
        }
    }

    (fabricEntries + neoEntries)
            .filter {
                it.startsWith("net/minecraft/")
                        || it.startsWith("net/fabricmc/")
                        || it.startsWith("net/neoforged/")
            }
            .forEach { violations += "distribution jar bundles external class $it" }

    return violations
}
