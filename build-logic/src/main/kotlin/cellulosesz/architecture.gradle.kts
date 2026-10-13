import cellulosesz.checks.architectureViolations

tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Enforces CellulosesZ source-level architecture rules across modules and loaders."
    // No declared inputs: this is a source scan and must not be tied to build output locations.
    dependsOn("verifyCommandCatalog", "verifyTranslations", "verifyHelpCopy")

    doLast {
        val violations = architectureViolations(
            rootProject.projectDir,
            rootProject.file("platform"),
        )
        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ architecture violations:\n - ${violations.joinToString("\n - ")}"
            )
        }
    }
}
