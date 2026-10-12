import cellulosesz.checks.commandCatalogTable
import cellulosesz.checks.commandCatalogViolations
import cellulosesz.checks.replaceCommandCatalog

tasks.register("generateCommandCatalog") {
    group = "verification"
    description = "Regenerates the README command catalog from the command DSL declarations."
    doLast {
        val readme = rootProject.file("README.md")
        val table = commandCatalogTable(rootProject.projectDir)
        readme.writeText(replaceCommandCatalog(readme.readText(), table))
        logger.lifecycle("README command catalog regenerated")
    }
}

tasks.register("verifyCommandCatalog") {
    group = "verification"
    description = "Validates command declarations, help documents and README catalog freshness."
    doLast {
        val violations = commandCatalogViolations(rootProject.projectDir)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ command catalog violations:\n - ${violations.joinToString("\n - ")}"
            )
        }
    }
}
