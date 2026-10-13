import cellulosesz.checks.helpCopyViolations

tasks.register("verifyHelpCopy") {
    group = "verification"
    description = "Structural checks for command help Markdown and suspended phrasings."
    doLast {
        val violations = helpCopyViolations(rootProject.projectDir)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "CellulosesZ help-copy violations:\n - ${violations.joinToString("\n - ")}"
            )
        }
    }
}
