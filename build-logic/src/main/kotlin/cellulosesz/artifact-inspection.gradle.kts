import cellulosesz.checks.artifactViolations

tasks.register("inspectArtifacts") {
    group = "verification"
    description = "Inspects both loader distributions (flattened modules, nested runtime jars, resources)."
    dependsOn("verifyArchitecture", "checkModuleDependencies")
    dependsOn(":26.1.2-fabric:build", ":26.1.2-neoforge:build")

    doLast {
        val violations = artifactViolations(rootProject.projectDir)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ artifact inspection violations:\n - ${violations.joinToString("\n - ")}"
            )
        }
    }
}
