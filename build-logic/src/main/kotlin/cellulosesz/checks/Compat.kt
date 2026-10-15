package cellulosesz.checks

import java.io.File
import java.util.zip.ZipFile

/** One structural assertion produced by the compatibility smoke. */
data class CompatCheck(
    val id: String,
    val passed: Boolean,
    val detail: String,
)

/**
 * Structural compatibility checks over a built loader distribution. These are the L1-structural
 * assertions that can run without launching a Minecraft server: the loader metadata is present, the
 * mixin configs ship, no loader/Minecraft classes are flattened, and CellulosesZ's own classes are
 * present. A live-server smoke (login, command routes, teleport cancel) is a separate, heavier run
 * described in `integration-tests/README.md`.
 */
fun compatStructuralChecks(root: File, loader: String): List<CompatCheck> {
    val checks = mutableListOf<CompatCheck>()
    val jar = root.resolve("versions/26.1.2-$loader/build/libs")
            .takeIf { it.isDirectory }
            ?.listFiles()
            ?.firstOrNull { it.name.endsWith("-$loader.jar") }

    checks += CompatCheck(
        "distribution_jar_present",
        jar != null,
        jar?.name ?: "missing"
    )

    if (jar != null) {
        val entries = zipEntries(jar)
        val metadata = if (loader == "fabric") "fabric.mod.json" else "META-INF/neoforge.mods.toml"
        checks += CompatCheck(
            "loader_metadata_present",
            metadata in entries,
            metadata
        )
        checks += CompatCheck(
            "mixin_configs_present",
            entries.any { it.endsWith(".mixins.json") },
            entries.filter { it.endsWith(".mixins.json") }.sorted().joinToString(", "),
        )
        checks += CompatCheck(
            "no_minecraft_classes_flattened",
            entries.none { it.startsWith("net/minecraft/") },
            "net/minecraft/ entries: ${entries.count { it.startsWith("net/minecraft/") }}",
        )
        checks += CompatCheck(
            "command_registry_present",
            entries.any { it.endsWith("core/command/registry/CommandRegistry.class") },
            "CommandRegistry.class",
        )
        checks += CompatCheck(
            "jail_restriction_bridge_present",
            entries.any { it.endsWith("jail/JailRestrictionSnapshot.class") },
            "JailRestrictionSnapshot.class",
        )
        checks += CompatCheck(
            "api_classes_present",
            entries.any { it.endsWith("api/CellulosesZApi.class") },
            "top/likoslupus/cellulosesz/api/CellulosesZApi.class",
        )
        checks += CompatCheck(
            "api_not_shipped_as_nested_jar",
            entries.none { it.contains("cellulosesz-api") && it.endsWith(".jar") },
            "no nested cellulosesz-api jar",
        )
    }

    return checks
}

/** Writes a machine-readable compatibility report and returns its path. */
fun writeCompatReport(
    root: File,
    loader: String,
    checks: List<CompatCheck>
): File {
    val reportDir = root.resolve("build/reports/compatibility")
    reportDir.mkdirs()
    val report = reportDir.resolve("$loader.json")
    val status = if (checks.all { it.passed }) "PASS" else "FAIL"
    val assertions = checks.joinToString(",\n    ") { check ->
        /* language=JSON */ """
        {
          "id": "${check.id}",
          "passed": ${check.passed},
          "detail": "${escape(check.detail)}"
        }
        """.trimIndent()
    }
    report.writeText(
        /* language=JSON */ """
        {
          "schemaVersion": 1,
          "minecraftVersion": "26.1.2",
          "loader": "$loader",
          "javaVersion": "25",
          "kind": "L1-structural",
          "status": "$status",
          "assertions": [
            $assertions
          ]
        }
        """.trimIndent() + "\n"
    )
    return report
}

private fun zipEntries(jar: File): List<String> =
    ZipFile(jar).use { zip ->
        zip.entries()
                .asSequence()
                .map { it.name }
                .toList()
    }

private fun escape(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")
