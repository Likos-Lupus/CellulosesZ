import cellulosesz.checks.compatStructuralChecks
import cellulosesz.checks.writeCompatReport

// Structural compatibility smoke for both loader distributions. It validates the built artifacts
// without launching a Minecraft server; the heavier live-server scenarios live under
// `integration-tests/` and are documented in `integration-tests/README.md`. Failures are never
// masked with continue-on-error.
val compatLoaders = listOf(
    Triple("Fabric", "fabric", "26.1.2-fabric"),
    Triple("NeoForge", "neoforge", "26.1.2-neoforge"),
)

compatLoaders.forEach { (suffix, loader, cell) ->
    tasks.register("compatSmoke$suffix") {
        group = "verification"
        description = "Structural compatibility smoke for the $loader distribution."
        dependsOn(":$cell:build")

        doLast {
            val checks = compatStructuralChecks(rootProject.projectDir, loader)
            val report = writeCompatReport(rootProject.projectDir, loader, checks)
            val failed = checks.filterNot { it.passed }
            if (failed.isNotEmpty()) {
                throw GradleException(
                    "compatibility smoke ($loader) failed:\n - " +
                            failed.joinToString("\n - ") { "${it.id}: ${it.detail}" } +
                            "\nreport: $report"
                )
            }
            logger.lifecycle("compatibility smoke ($loader) passed; report: $report")
        }
    }
}

tasks.register("compatSmokeBothLoaders") {
    group = "verification"
    description = "Runs the structural compatibility smoke for both loader distributions."
    dependsOn("compatSmokeFabric", "compatSmokeNeoForge")
}
