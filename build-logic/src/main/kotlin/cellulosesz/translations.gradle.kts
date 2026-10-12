import cellulosesz.checks.translationViolations

tasks.register("verifyTranslations") {
    group = "verification"
    description = "Validates per-module translation key and placeholder parity."
    doLast {
        val violations = translationViolations(rootProject.projectDir)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ translation violations:\n - ${violations.joinToString("\n - ")}"
            )
        }
    }
}
